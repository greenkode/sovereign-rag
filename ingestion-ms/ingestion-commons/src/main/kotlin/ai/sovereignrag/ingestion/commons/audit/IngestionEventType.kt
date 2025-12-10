package ai.sovereignrag.ingestion.commons.audit

enum class IngestionEventType(val description: String) {
    FILE_UPLOAD_INITIATED("File upload initiated"),
    FILE_UPLOAD_CONFIRMED("File upload confirmed and queued"),
    FILE_UPLOAD_COMPLETED("File upload processing completed"),
    FILE_UPLOAD_FAILED("File upload processing failed"),

    BATCH_UPLOAD_INITIATED("Batch upload initiated"),
    BATCH_UPLOAD_CONFIRMED("Batch upload confirmed and queued"),
    BATCH_UPLOAD_COMPLETED("Batch upload processing completed"),
    BATCH_UPLOAD_FAILED("Batch upload processing failed"),

    FOLDER_UPLOAD_INITIATED("Folder/ZIP upload initiated"),
    FOLDER_UPLOAD_CONFIRMED("Folder upload confirmed and queued"),
    FOLDER_UPLOAD_COMPLETED("Folder upload processing completed"),
    FOLDER_UPLOAD_FAILED("Folder upload processing failed"),

    WEB_SCRAPE_INITIATED("Web scrape job initiated"),
    WEB_SCRAPE_COMPLETED("Web scrape processing completed"),
    WEB_SCRAPE_FAILED("Web scrape processing failed"),

    TEXT_INPUT_SUBMITTED("Text input submitted"),
    TEXT_INPUT_COMPLETED("Text input processing completed"),
    TEXT_INPUT_FAILED("Text input processing failed"),

    QA_PAIRS_SUBMITTED("Q&A pairs submitted"),
    QA_PAIRS_COMPLETED("Q&A pairs processing completed"),
    QA_PAIRS_FAILED("Q&A pairs processing failed"),

    RSS_FEED_SUBMITTED("RSS feed submitted"),
    RSS_FEED_COMPLETED("RSS feed processing completed"),
    RSS_FEED_FAILED("RSS feed processing failed"),

    JOB_CANCELLED("Ingestion job cancelled"),
    JOB_RETRIED("Ingestion job retried"),

    EMBEDDING_STARTED("Embedding generation started"),
    EMBEDDING_COMPLETED("Embedding generation completed"),
    EMBEDDING_FAILED("Embedding generation failed"),

    KNOWLEDGE_SOURCE_CREATED("Knowledge source created"),
    KNOWLEDGE_SOURCE_DELETED("Knowledge source deleted")
}
