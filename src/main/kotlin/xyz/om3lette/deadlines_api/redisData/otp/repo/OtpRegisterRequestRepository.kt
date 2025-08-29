package xyz.om3lette.deadlines_api.redisData.otp.repo

import org.springframework.data.repository.CrudRepository
import xyz.om3lette.deadlines_api.redisData.otp.model.OtpRegisterRequest
import java.util.UUID

interface OtpRegisterRequestRepository : CrudRepository<OtpRegisterRequest, UUID>{
}