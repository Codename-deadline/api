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

        assertEquals(listOf(TimeRemaining.FIFTEEN_MINUTES), savedNotifications.captured.map { it.type })
        assertEquals(now.plusSeconds(15 * 60), savedNotifications.captured.single().sendAt)
    }

    @Test
    fun `reconcileNotifications updates pending reminders that still belong to the plan`() {
        deadline.due = now.plusSeconds(2 * 60 * 60)
        val pendingFifteenMinutes = notification(TimeRemaining.FIFTEEN_MINUTES, now.plusSeconds(5 * 60), NotificationStatus.PENDING)
        val pendingOneHour = notification(TimeRemaining.ONE_HOUR, now.plusSeconds(10 * 60), NotificationStatus.PENDING)
        val savedNotifications = slot<List<DeadlineNotification>>()

        every {
            deadlineNotificationRepository.findAllByDeadlineAndStatus(deadline, NotificationStatus.PENDING)
        } returns listOf(pendingFifteenMinutes, pendingOneHour)
        every { deadlineNotificationRepository.saveAll(capture(savedNotifications)) } returnsArgument 0

        plannerService.reconcileNotifications(deadline, now)

        assertEquals(now.plusSeconds(105 * 60), pendingFifteenMinutes.sendAt)
        assertEquals(now.plusSeconds(60 * 60), pendingOneHour.sendAt)
        assertEquals(listOf(pendingFifteenMinutes, pendingOneHour), savedNotifications.captured)
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
        assertEquals(listOf(TimeRemaining.FIFTEEN_MINUTES), savedNotifications.captured.map { it.type })
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
        assertEquals(listOf(TimeRemaining.FIFTEEN_MINUTES), savedNotifications.captured.map { it.type })
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
