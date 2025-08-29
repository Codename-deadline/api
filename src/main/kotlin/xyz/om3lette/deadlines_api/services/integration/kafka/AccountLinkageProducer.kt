package xyz.om3lette.deadlines_api.services.integration.kafka

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.event.UserMessengerAccountLinkageEvent
import xyz.om3lette.deadlines_api.services.BaseProducer

@Service
class AccountLinkageProducer(
    accountLinkageKafkaTemplate: KafkaTemplate<String, UserMessengerAccountLinkageEvent>
) : BaseProducer<UserMessengerAccountLinkageEvent>("private.integration.accountlinkage", accountLinkageKafkaTemplate)
