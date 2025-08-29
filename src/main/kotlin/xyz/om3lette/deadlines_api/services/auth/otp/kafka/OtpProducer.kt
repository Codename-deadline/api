package xyz.om3lette.deadlines_api.services.auth.otp.kafka

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.otp.event.OtpEvent
import xyz.om3lette.deadlines_api.services.BaseProducer

@Service
class OtpProducer(otpKafkaTemplate: KafkaTemplate<String, OtpEvent>) :
    BaseProducer<OtpEvent>("private.auth.otp", otpKafkaTemplate)