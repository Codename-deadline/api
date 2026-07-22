package xyz.om3lette.deadlines_api.services.notifications

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.data.notifications.enums.NotificationStatus
import xyz.om3lette.deadlines_api.data.notifications.enums.TimeRemaining
import xyz.om3lette.deadlines_api.data.notifications.model.DeadlineNotification
import xyz.om3lette.deadlines_api.data.notifications.repo.DeadlineNotificationRepository
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeadlineNotificationPlannerServiceTest {
    private val deadlineNotificationRepository: DeadlineNotificationRepository = mockk()
    private val plannerService = DeadlineNotificationPlannerService(deadlineNotificationRepository)

    private lateinit var now: Instant
    private lateinit var deadline: Deadline

    @BeforeEach
    fun commonFixtures() {
        now = Instant.parse("2026-07-07T12:00:00Z")
        val organization = DomainObjectBuilder.organization()
        val thread = DomainObjectBuilder.thread(organization)
        deadline = DomainObjectBuilder.deadline(thread, due = now.plusSeconds(30 * 60))
    }

    @Test
    fun `createNotifications creates only reminders with future sendAt`() {
        val savedNotifications = slot<List<DeadlineNotification>>()
        every { deadlineNotificationRepository.saveAll(capture(savedNotifications)) } returnsArgument 0

        plannerService.createNotifications(deadline, now)

        assertEquals(
            listOf(TimeRemaining.FIFTEEN_MINUTES, TimeRemaining.NO_TIME),
            savedNotifications.captured.map { it.type }
        )
        assertEquals(
            now.plusSeconds(15 * 60),
            savedNotifications.captured.single { it.type == TimeRemaining.FIFTEEN_MINUTES }.sendAt
        )
        assertEquals(deadline.due, savedNotifications.captured.single { it.type == TimeRemaining.NO_TIME }.sendAt)
    }

    @Test
    fun `reconcileNotifications updates pending reminders that still belong to the plan`() {
        deadline.due = now.plusSeconds(2 * 60 * 60)
        val pendingFifteenMinutes = notification(TimeRemaining.FIFTEEN_MINUTES, now.plusSeconds(5 * 60), NotificationStatus.PENDING)
        val pendingOneHour = notification(TimeRemaining.ONE_HOUR, now.plusSeconds(10 * 60), NotificationStatus.PENDING)
        val pendingNoTime = notification(TimeRemaining.NO_TIME, now.plusSeconds(20 * 60), NotificationStatus.PENDING)
        val savedNotifications = slot<List<DeadlineNotification>>()

        every {
            deadlineNotificationRepository.findAllByDeadlineAndStatus(deadline, NotificationStatus.PENDING)
        } returns listOf(pendingFifteenMinutes, pendingOneHour, pendingNoTime)
        every { deadlineNotificationRepository.saveAll(capture(savedNotifications)) } returnsArgument 0

        plannerService.reconcileNotifications(deadline, now)

        assertEquals(now.plusSeconds(105 * 60), pendingFifteenMinutes.sendAt)
        assertEquals(now.plusSeconds(60 * 60), pendingOneHour.sendAt)
        assertEquals(now.plusSeconds(2 * 60 * 60), pendingNoTime.sendAt)
        assertEquals(listOf(pendingFifteenMinutes, pendingOneHour, pendingNoTime), savedNotifications.captured)
    }

    @Test
    fun `reconcileNotifications deletes pending reminders whose planned sendAt is no longer future`() {
        deadline.due = now.plusSeconds(30 * 60)
        val pendingOneHour = notification(TimeRemaining.ONE_HOUR, now.plusSeconds(10 * 60), NotificationStatus.PENDING)
        val deletedNotifications = slot<List<DeadlineNotification>>()
        val savedNotifications = slot<List<DeadlineNotification>>()

        every {
            deadlineNotificationRepository.findAllByDeadlineAndStatus(deadline, NotificationStatus.PENDING)
        } returns listOf(pendingOneHour)
        every { deadlineNotificationRepository.deleteAll(capture(deletedNotifications)) } returns Unit
        every { deadlineNotificationRepository.saveAll(capture(savedNotifications)) } returnsArgument 0

        plannerService.reconcileNotifications(deadline, now)

        assertEquals(listOf(pendingOneHour), deletedNotifications.captured)
        assertEquals(
            listOf(TimeRemaining.FIFTEEN_MINUTES, TimeRemaining.NO_TIME),
            savedNotifications.captured.map { it.type }
        )
    }

    @Test
    fun `reconcileNotifications creates future reminder even if old reminder of same type was already sent`() {
        deadline.due = now.plusSeconds(2 * 24 * 60 * 60)
        val sentOneDay = notification(TimeRemaining.ONE_DAY, now.minusSeconds(60), NotificationStatus.SENT)
        val savedNotifications = slot<List<DeadlineNotification>>()

        every {
            deadlineNotificationRepository.findAllByDeadlineAndStatus(deadline, NotificationStatus.PENDING)
        } returns emptyList()
        every { deadlineNotificationRepository.saveAll(capture(savedNotifications)) } returnsArgument 0

        plannerService.reconcileNotifications(deadline, now)

        assertTrue(savedNotifications.captured.any { it.type == TimeRemaining.ONE_DAY })
        assertEquals(NotificationStatus.SENT, sentOneDay.status)
        assertEquals(now.minusSeconds(60), sentOneDay.sendAt)
    }

    @Test
    fun `reconcileNotifications does not reset non-pending reminders`() {
        deadline.due = now.plusSeconds(30 * 60)
        val inProgressOneHour = notification(TimeRemaining.ONE_HOUR, now.minusSeconds(60), NotificationStatus.IN_PROGRESS)
        val savedNotifications = slot<List<DeadlineNotification>>()

        every {
            deadlineNotificationRepository.findAllByDeadlineAndStatus(deadline, NotificationStatus.PENDING)
        } returns emptyList()
        every { deadlineNotificationRepository.saveAll(capture(savedNotifications)) } returnsArgument 0

        plannerService.reconcileNotifications(deadline, now)

        verify(exactly = 0) { deadlineNotificationRepository.deleteAll(any<List<DeadlineNotification>>()) }
        assertEquals(NotificationStatus.IN_PROGRESS, inProgressOneHour.status)
        assertEquals(now.minusSeconds(60), inProgressOneHour.sendAt)
        assertEquals(
            listOf(TimeRemaining.FIFTEEN_MINUTES, TimeRemaining.NO_TIME),
            savedNotifications.captured.map { it.type }
        )
    }

    @Test
    fun `all notification times are rounded down to whole minutes`() {
        deadline.due = Instant.parse("2026-08-20T23:59:59.999999999Z")
        val savedNotifications = slot<List<DeadlineNotification>>()
        every { deadlineNotificationRepository.saveAll(capture(savedNotifications)) } returnsArgument 0

        plannerService.createNotifications(deadline, now)

        assertEquals(TimeRemaining.entries.toSet(), savedNotifications.captured.map { it.type }.toSet())
        assertTrue(savedNotifications.captured.all { it.sendAt.nano == 0 })
        assertTrue(savedNotifications.captured.all { it.sendAt.epochSecond % 60 == 0L })
        assertEquals(
            deadline.due.truncatedTo(ChronoUnit.MINUTES),
            savedNotifications.captured.single { it.type == TimeRemaining.NO_TIME }.sendAt
        )
    }

    @Test
    fun `deleteNotifications removes every notification for deadline`() {
        every { deadlineNotificationRepository.deleteAllByDeadline(deadline) } returns 3

        plannerService.deleteNotifications(deadline)

        verify(exactly = 1) { deadlineNotificationRepository.deleteAllByDeadline(deadline) }
    }

    private fun notification(
        type: TimeRemaining,
        sendAt: Instant,
        status: NotificationStatus
    ) = DeadlineNotification(
        id = type.ordinal.toLong() + 1,
        deadline = deadline,
        sendAt = sendAt,
        type = type,
        status = status
    )
}
