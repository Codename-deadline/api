package xyz.om3lette.deadlines_api.redisData.otp.repo

import org.springframework.data.repository.CrudRepository
import xyz.om3lette.deadlines_api.redisData.otp.model.OtpPasswordCheck
import java.util.UUID

interface OtpPasswordCheckRepository : CrudRepository<OtpPasswordCheck, UUID> {
}