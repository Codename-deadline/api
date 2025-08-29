package xyz.om3lette.deadlines_api.data.outbox.repo

import org.springframework.data.jpa.repository.JpaRepository
import xyz.om3lette.deadlines_api.data.outbox.model.Outbox

interface OutboxRepository : JpaRepository<Outbox, Long>, OutboxCustomRepository {
}