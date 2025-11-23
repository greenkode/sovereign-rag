//package ai.sovereignrag.audit.listener
//
//import ai.sovereignrag.audit.event.AuditLogEvent
//import ai.sovereignrag.audit.domain.model.AuditLogEntity
//import ai.sovereignrag.audit.domain.model.AuditLogRepository
//import io.github.oshai.kotlinlogging.KotlinLogging
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import org.springframework.kafka.annotation.DltHandler
//import org.springframework.kafka.annotation.KafkaListener
//import org.springframework.kafka.annotation.RetryableTopic
//import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy
//import org.springframework.kafka.support.Acknowledgment
//import org.springframework.kafka.support.KafkaHeaders
//import org.springframework.messaging.MessageHeaders
//import org.springframework.messaging.handler.annotation.Header
//import org.springframework.messaging.handler.annotation.Headers
//import org.springframework.retry.annotation.Backoff
//import org.springframework.stereotype.Component
//
//// @Component - Kafka disabled
//class AuditLogEventConsumer(private val auditLogRepository: AuditLogRepository) {
//
//    private val log = KotlinLogging.logger { }
//
//    @RetryableTopic(
//        backoff = Backoff(value = 6000),
//        attempts = "4",
//        autoCreateTopics = "false",
//        retryTopicSuffix = "-retry",
//        dltTopicSuffix = "-dlt",
//        sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
//        exclude = [NullPointerException::class, IllegalArgumentException::class]
//    )
//    @KafkaListener(topics = ["\${application.kafka.topics.audit-logs}"])
//    fun consume(data: ConsumerRecord<String, AuditLogEvent>, @Headers headers: MessageHeaders) {
//        log.debug { "Received event id ${data.key()} from topic ${data.topic()}" }
//        try {
//
//            val domain = data.value().toDomain()
//            auditLogRepository.save(
//                AuditLogEntity(
//                    domain.id, domain.identity, domain.identityType, domain.resource, domain.event,
//                    domain.eventTime, domain.timeRecorded, domain.payload
//                )
//            )
//
//            val acknowledgment = headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment::class.java)
//            acknowledgment?.acknowledge()
//        } catch (ex: Exception) {
//            log.error { "Error while consuming audit log: $ex" }
//        }
//    }
//
//    @DltHandler
//    fun dlt(data: AuditLogEvent?, @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String?) {
//        println("Event from topic $topic  is dead lettered - event: $data")
//    }
//}
