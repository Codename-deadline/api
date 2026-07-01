package xyz.om3lette.deadlines_api.configs

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import xyz.om3lette.deadlines_api.configs.properties.KafkaProperties
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.event.UserMessengerAccountLinkageEvent
import xyz.om3lette.deadlines_api.data.notifications.event.DeadlineNotificationEvent
import xyz.om3lette.deadlines_api.data.otp.event.OtpEvent


@Configuration
class KafkaProducerConfig(
    kafkaProperties: KafkaProperties
) {

    private val configProps: MutableMap<String, Any> = mutableMapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG  to kafkaProperties.bootstrapServers,
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
        ProducerConfig.COMPRESSION_TYPE_CONFIG   to "gzip",
    )

    @Bean
    fun notificationProducerFactory(): ProducerFactory<String, DeadlineNotificationEvent> =
        DefaultKafkaProducerFactory(
            configProps,
            StringSerializer(),
            JacksonJsonSerializer<DeadlineNotificationEvent>().apply {
                isAddTypeInfo = false
            }
        )

    @Bean
    fun accountLinkageProducerFactory(): ProducerFactory<String, UserMessengerAccountLinkageEvent> =
        DefaultKafkaProducerFactory(
            configProps,
            StringSerializer(),
            JacksonJsonSerializer<UserMessengerAccountLinkageEvent>().apply {
                isAddTypeInfo = false
            }
        )

    @Bean
    fun otpProducerFactory(): ProducerFactory<String, OtpEvent> =
        DefaultKafkaProducerFactory(
            configProps,
            StringSerializer(),
            JacksonJsonSerializer<OtpEvent>().apply {
                isAddTypeInfo = false
            }
        )

    @Bean
    fun notificationKafkaTemplate(): KafkaTemplate<String, DeadlineNotificationEvent> =
        KafkaTemplate(notificationProducerFactory())

    @Bean
    fun accountLinkageKafkaTemplate(): KafkaTemplate<String, UserMessengerAccountLinkageEvent> =
        KafkaTemplate(accountLinkageProducerFactory())

    @Bean
    fun otpKafkaTemplate(): KafkaTemplate<String, OtpEvent> =
        KafkaTemplate(otpProducerFactory())
}
