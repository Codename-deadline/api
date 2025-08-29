package xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.repo

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import xyz.om3lette.deadlines_api.redisData.integration.messengerAccount.model.AccountLinkageRequest

@Repository
interface AccountLinkageRepository : CrudRepository<AccountLinkageRequest, String> {
}