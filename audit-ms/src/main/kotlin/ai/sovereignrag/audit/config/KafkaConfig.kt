package ai.sovereignrag.audit.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// @Configuration - Kafka disabled
class KafkaConfig {

    // @Bean
    fun auditLogsRetry() = NewTopic("bml-audit-logs-retry", 5, 1)

    // @Bean
    fun auditLogsDlt() = NewTopic("bml-audit-logs-dlt", 5, 1)
}