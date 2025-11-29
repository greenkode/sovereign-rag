CREATE TABLE IF NOT EXISTS tenants
(
    id                   VARCHAR(255) NOT NULL PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    database_name        VARCHAR(255) NOT NULL UNIQUE,
    api_key_hash         VARCHAR(512) NOT NULL,
    status               VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    client_id            VARCHAR(100) NOT NULL,
    max_documents        INTEGER      NOT NULL DEFAULT 10000,
    max_embeddings       INTEGER      NOT NULL DEFAULT 50000,
    max_requests_per_day INTEGER      NOT NULL DEFAULT 10000,
    contact_email        VARCHAR(500),
    contact_name         VARCHAR(500),
    admin_email          VARCHAR(255),
    wordpress_url        VARCHAR(1000),
    wordpress_version    VARCHAR(50),
    plugin_version       VARCHAR(50),
    features             JSONB        NOT NULL DEFAULT '{}',
    settings             JSONB        NOT NULL DEFAULT '{}',
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_active_at       TIMESTAMP,
    deleted_at           TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status);
CREATE INDEX IF NOT EXISTS idx_tenants_client_id ON tenants(client_id);
CREATE INDEX IF NOT EXISTS idx_tenants_database_name ON tenants(database_name);

CREATE TABLE IF NOT EXISTS groups
(
    id                 SERIAL       NOT NULL PRIMARY KEY,
    name               VARCHAR(255) UNIQUE NOT NULL,
    description        VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS authority
(
    id                 SERIAL       NOT NULL PRIMARY KEY,
    name               VARCHAR(255) UNIQUE NOT NULL,
    description        VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS group_authorities
(
    group_id     INTEGER NOT NULL,
    authority_id INTEGER NOT NULL,
    PRIMARY KEY (group_id, authority_id),
    CONSTRAINT fk_group_authorities_group_id FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT fk_group_authorities_authority_id FOREIGN KEY (authority_id) REFERENCES authority (id)
);

CREATE TABLE IF NOT EXISTS event_publication
(
    id               UUID                     NOT NULL PRIMARY KEY,
    listener_id      TEXT                     NOT NULL,
    event_type       TEXT                     NOT NULL,
    serialized_event TEXT                     NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE
);
CREATE TABLE IF NOT EXISTS process
(
    id                    BIGSERIAL    NOT NULL PRIMARY KEY,
    type                  VARCHAR(255) NOT NULL,
    description           VARCHAR(255) NOT NULL,
    state                 VARCHAR(255) NOT NULL,
    public_id             UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    expiry                TIMESTAMP    NOT NULL DEFAULT NOW(),
    external_reference    VARCHAR(255),
    integrator_reference  VARCHAR(1000) NULL,
    channel               VARCHAR(255) NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT unique_process_type_external_reference UNIQUE (type, external_reference)
);

CREATE INDEX IF NOT EXISTS idx_process_public_id ON process(public_id);

CREATE TABLE IF NOT EXISTS process_request
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    process_id         BIGINT       NOT NULL,
    user_id            UUID         NOT NULL,
    type               VARCHAR(255) NOT NULL,
    state              VARCHAR(255) NOT NULL,
    channel            VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_process_request_process FOREIGN KEY (process_id) REFERENCES process (id)
);

CREATE TABLE IF NOT EXISTS process_request_data
(
    process_request_id BIGINT       NOT NULL,
    name               VARCHAR(255) NOT NULL,
    value              TEXT         NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (process_request_id, name),
    CONSTRAINT fk_process_request_data FOREIGN KEY (process_request_id) REFERENCES process_request (id)
);

CREATE TABLE IF NOT EXISTS process_request_stakeholder
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    process_request_id BIGINT       NOT NULL,
    stakeholder_id     VARCHAR(100) NOT NULL,
    type               VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_process_request_stakeholder FOREIGN KEY (process_request_id) REFERENCES process_request (id)
);

CREATE TABLE IF NOT EXISTS process_event_transition
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    process_id         BIGINT       NOT NULL,
    event              VARCHAR(255) NOT NULL,
    user_id            UUID         NOT NULL,
    old_state          VARCHAR(255),
    new_state          VARCHAR(255),
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_process_event_transition_process FOREIGN KEY (process_id) REFERENCES process (id)
);

CREATE TABLE IF NOT EXISTS subscription_limit
(
    id                  SERIAL       NOT NULL PRIMARY KEY,
    tenant_id           UUID         NOT NULL,
    subscription_tier   VARCHAR(50)  NOT NULL,
    daily_token_limit   BIGINT       NOT NULL,
    monthly_token_limit BIGINT       NOT NULL,
    start               TIMESTAMP    NOT NULL,
    expiry              TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by    VARCHAR(255) NOT NULL DEFAULT 'system',
    version             BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_subscription_limit_tenant_id ON subscription_limit(tenant_id);
CREATE INDEX IF NOT EXISTS idx_subscription_limit_tenant_active ON subscription_limit(tenant_id, start, expiry);

CREATE TABLE IF NOT EXISTS access_token
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    type               VARCHAR(255) NOT NULL,
    expiry             TIMESTAMP    NOT NULL,
    access_token       TEXT         NOT NULL,
    refresh_token      TEXT         NULL,
    resource           VARCHAR(255) NOT NULL,
    institution        VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notification_device
(
    id                   BIGSERIAL PRIMARY KEY,
    public_id            UUID         NOT NULL,
    notification_channel VARCHAR(50)  NOT NULL,
    value                TEXT         NOT NULL,
    user_id              UUID         NOT NULL,
    notification_type    VARCHAR(50)  NOT NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by     VARCHAR(255) NOT NULL DEFAULT 'system',
    version              BIGINT       NOT NULL DEFAULT 0,
    UNIQUE (notification_type, notification_channel, user_id)
);

CREATE TABLE IF NOT EXISTS system_property
(
    id                 SERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    scope              VARCHAR(255) NOT NULL,
    value              TEXT         NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT system_property_unique_name_scope UNIQUE (name, scope)
);

CREATE TABLE IF NOT EXISTS users
(
    id                 BIGSERIAL PRIMARY KEY,
    public_id          UUID         NOT NULL UNIQUE,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_property
(
    user_id        BIGINT      NOT NULL,
    property_name  VARCHAR(50) NOT NULL,
    property_value VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT unique_user_id_name UNIQUE (user_id, property_name)
);

CREATE TABLE IF NOT EXISTS user_external_id
(
    user_id         BIGINT      NOT NULL,
    integrator_code VARCHAR(50) NOT NULL,
    integrator      VARCHAR(50) NOT NULL,
    external_id     VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT unique_user_id_integrator_code UNIQUE (user_id, integrator_code)
);

CREATE TABLE IF NOT EXISTS webhook_configuration
(
    id                 BIGSERIAL PRIMARY KEY,
    public_id          UUID          NOT NULL UNIQUE,
    merchant_id        UUID          NOT NULL,
    webhook_url        VARCHAR(2048) NOT NULL,
    notification_type  VARCHAR(50)   NOT NULL,
    is_active          BOOLEAN       NOT NULL DEFAULT TRUE,
    secret_key         VARCHAR(512),
    custom_headers     TEXT,
    retry_attempts     INTEGER       NOT NULL DEFAULT 3,
    timeout_seconds    INTEGER       NOT NULL DEFAULT 30,
    description        TEXT,
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    last_modified_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255)  NOT NULL DEFAULT 'system',
    last_modified_by   VARCHAR(255)  NOT NULL DEFAULT 'system',
    version            BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_webhook_configuration_merchant_id ON webhook_configuration(merchant_id);
CREATE INDEX IF NOT EXISTS idx_webhook_configuration_merchant_active ON webhook_configuration(merchant_id, is_active);
CREATE INDEX IF NOT EXISTS idx_webhook_configuration_merchant_type ON webhook_configuration(merchant_id, notification_type);
CREATE INDEX IF NOT EXISTS idx_webhook_configuration_merchant_type_active ON webhook_configuration(merchant_id, notification_type, is_active);
CREATE INDEX IF NOT EXISTS idx_webhook_configuration_public_id ON webhook_configuration(public_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_webhook_configuration_unique ON webhook_configuration(merchant_id, webhook_url, notification_type);

CREATE TABLE IF NOT EXISTS webhook_delivery_log
(
    id                       BIGSERIAL PRIMARY KEY,
    webhook_configuration_id BIGINT       NOT NULL,
    event_id                 VARCHAR(255) NOT NULL,
    event_type               VARCHAR(100) NOT NULL,
    webhook_url              VARCHAR(2048) NOT NULL,
    payload                  TEXT         NOT NULL,
    http_status_code         INTEGER,
    response_body            TEXT,
    response_headers         TEXT,
    delivery_status          VARCHAR(20)  NOT NULL,
    attempts_count           INTEGER      NOT NULL DEFAULT 1,
    total_duration_ms        BIGINT,
    error_message            TEXT,
    delivered_at             TIMESTAMP,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_delivery_log_configuration FOREIGN KEY (webhook_configuration_id) REFERENCES webhook_configuration(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_config_id ON webhook_delivery_log(webhook_configuration_id);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_event_id ON webhook_delivery_log(event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_status ON webhook_delivery_log(delivery_status);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_created_at ON webhook_delivery_log(created_at);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_config_status_date ON webhook_delivery_log(webhook_configuration_id, delivery_status, created_at);

CREATE TABLE IF NOT EXISTS QRTZ_JOB_DETAILS
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(200) NOT NULL,
    JOB_GROUP         VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(250) NULL,
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        BOOL         NOT NULL,
    IS_NONCONCURRENT  BOOL         NOT NULL,
    IS_UPDATE_DATA    BOOL         NOT NULL,
    REQUESTS_RECOVERY BOOL         NOT NULL,
    JOB_DATA          BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_TRIGGERS
(
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(200) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    JOB_NAME       VARCHAR(200) NOT NULL,
    JOB_GROUP      VARCHAR(200) NOT NULL,
    DESCRIPTION    VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT       NULL,
    PREV_FIRE_TIME BIGINT       NULL,
    PRIORITY       INTEGER      NULL,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT       NOT NULL,
    END_TIME       BIGINT       NULL,
    CALENDAR_NAME  VARCHAR(200) NULL,
    MISFIRE_INSTR  SMALLINT     NULL,
    JOB_DATA       BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP) REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_SIMPLE_TRIGGERS
(
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    REPEAT_COUNT    BIGINT       NOT NULL,
    REPEAT_INTERVAL BIGINT       NOT NULL,
    TIMES_TRIGGERED BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_CRON_TRIGGERS
(
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_SIMPROP_TRIGGERS
(
    SCHED_NAME    VARCHAR(120)   NOT NULL,
    TRIGGER_NAME  VARCHAR(200)   NOT NULL,
    TRIGGER_GROUP VARCHAR(200)   NOT NULL,
    STR_PROP_1    VARCHAR(512)   NULL,
    STR_PROP_2    VARCHAR(512)   NULL,
    STR_PROP_3    VARCHAR(512)   NULL,
    INT_PROP_1    INT            NULL,
    INT_PROP_2    INT            NULL,
    LONG_PROP_1   BIGINT         NULL,
    LONG_PROP_2   BIGINT         NULL,
    DEC_PROP_1    NUMERIC(13, 4) NULL,
    DEC_PROP_2    NUMERIC(13, 4) NULL,
    BOOL_PROP_1   BOOL           NULL,
    BOOL_PROP_2   BOOL           NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_BLOB_TRIGGERS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA     BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_CALENDARS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR      BYTEA        NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);

CREATE TABLE IF NOT EXISTS QRTZ_PAUSED_TRIGGER_GRPS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_FIRED_TRIGGERS
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(200) NOT NULL,
    TRIGGER_GROUP     VARCHAR(200) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    FIRED_TIME        BIGINT       NOT NULL,
    SCHED_TIME        BIGINT       NOT NULL,
    PRIORITY          INTEGER      NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(200) NULL,
    JOB_GROUP         VARCHAR(200) NULL,
    IS_NONCONCURRENT  BOOL         NULL,
    REQUESTS_RECOVERY BOOL         NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE IF NOT EXISTS QRTZ_SCHEDULER_STATE
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT       NOT NULL,
    CHECKIN_INTERVAL  BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE IF NOT EXISTS QRTZ_LOCKS
(
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE INDEX IF NOT EXISTS IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, JOB_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IF NOT EXISTS IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, TRIGGER_GROUP);
