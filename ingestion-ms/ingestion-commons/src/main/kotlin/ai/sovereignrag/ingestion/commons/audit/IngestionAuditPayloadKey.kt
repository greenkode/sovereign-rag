package ai.sovereignrag.ingestion.commons.audit

enum class IngestionAuditPayloadKey(val value: String) {
    ACTOR_ID("actor_id"),
    ACTOR_NAME("actor_name"),
    MERCHANT_ID("merchant_id"),
    IDENTITY_TYPE("identity_type"),
    RESOURCE("resource"),
    EVENT("event"),
    EVENT_TIME("event_time"),
    PAYLOAD("payload"),

    JOB_ID("job_id"),
    JOB_TYPE("job_type"),
    KNOWLEDGE_BASE_ID("knowledge_base_id"),
    KNOWLEDGE_SOURCE_ID("knowledge_source_id"),
    FILE_NAME("file_name"),
    FILE_SIZE("file_size"),
    MIME_TYPE("mime_type"),
    SOURCE_URL("source_url"),
    SOURCE_TYPE("source_type"),

    CHUNKS_CREATED("chunks_created"),
    BYTES_PROCESSED("bytes_processed"),
    PROCESSING_DURATION_MS("processing_duration_ms"),
    TOTAL_FILES("total_files"),
    COMPLETED_FILES("completed_files"),
    FAILED_FILES("failed_files"),

    ERROR_MESSAGE("error_message"),
    IP_ADDRESS("ip_address")
}
