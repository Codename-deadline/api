package xyz.om3lette.deadlines_api.data.attachments.repo

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import xyz.om3lette.deadlines_api.data.attachments.model.Attachment
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline

interface AttachmentRepository : JpaRepository<Attachment, Long> {
    fun findAllByDeadline(deadline: Deadline, pageable: Pageable): List<Attachment>
}