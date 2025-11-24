CREATE TABLE IF NOT EXISTS groups
(
    id                 SERIAL       NOT NULL PRIMARY KEY,
    name               VARCHAR(255) UNIQUE NOT NULL,
    description        VARCHAR(255) NOT NULL,
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS authority
(
    id                 SERIAL       NOT NULL PRIMARY KEY,
    name               VARCHAR(255) UNIQUE NOT NULL,
    description        VARCHAR(255) NOT NULL,
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
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

CREATE TABLE IF NOT EXISTS account_profile
(
    id                 SERIAL       NOT NULL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    description        VARCHAR(255) NOT NULL,
    public_id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    created_by         VARCHAR(50)  NOT NULL,
    created_date       TIMESTAMP             DEFAULT NULL,
    last_modified_by   VARCHAR(50)           DEFAULT NULL,
    last_modified_date TIMESTAMP             DEFAULT NULL,
    version            BIGINT       NOT NULL,
    CONSTRAINT ux_trust_level_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS account
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    name               VARCHAR(255),
    alias              VARCHAR(255),
    trust_level        VARCHAR(100),
    currency           VARCHAR(10),
    user_id            UUID         NOT NULL,
    type               VARCHAR(255),
    overdraft          NUMERIC               DEFAULT 0,
    parent_account_id  BIGINT,
    status             VARCHAR(50),
    is_default         BOOLEAN,
    profile_id         INTEGER,
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL,
    public_id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    CONSTRAINT ux_account_public_id_unique UNIQUE (public_id),
    CONSTRAINT fk_account_parent_account_id FOREIGN KEY (parent_account_id) REFERENCES account (id),
    CONSTRAINT fk_account_profile_id FOREIGN KEY (profile_id) REFERENCES account_profile (id)
);

CREATE TABLE IF NOT EXISTS account_property
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    value              TEXT         NOT NULL,
    account_id         BIGINT,
    scope              VARCHAR(255) NOT NULL,
    scope_value        VARCHAR(255) NOT NULL DEFAULT 'N/A',
    created_by         VARCHAR(50)  NOT NULL,
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(50),
    last_modified_date TIMESTAMP,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ux_property_scope UNIQUE (name, scope, scope_value),
    CONSTRAINT fk_account_property_account_id FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE IF NOT EXISTS account_address
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    address            VARCHAR(255) NOT NULL,
    type               VARCHAR(255) NOT NULL,
    platform           VARCHAR(255) NOT NULL,
    currency           VARCHAR(25)  NOT NULL,
    account_id         BIGINT       NOT NULL,
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL,
    CONSTRAINT fk_unique_address UNIQUE (address, type, platform),
    CONSTRAINT fk_account_address_account_id FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE IF NOT EXISTS account_address_property
(
    account_address_id BIGINT       NOT NULL,
    name               VARCHAR(255) NOT NULL,
    value              VARCHAR(255) NOT NULL,
    PRIMARY KEY (account_address_id, name),
    FOREIGN KEY (account_address_id) REFERENCES account_address (id)
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
    created_date          TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date    TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(255) NOT NULL,
    last_modified_by      VARCHAR(255) NOT NULL,
    version               BIGINT       NOT NULL,
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
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL,
    CONSTRAINT fk_process_request_process FOREIGN KEY (process_id) REFERENCES process (id)
);

CREATE TABLE IF NOT EXISTS process_request_data
(
    process_request_id BIGINT       NOT NULL,
    name               VARCHAR(255) NOT NULL,
    value              TEXT         NOT NULL,
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL,
    PRIMARY KEY (process_request_id, name),
    CONSTRAINT fk_process_request_data FOREIGN KEY (process_request_id) REFERENCES process_request (id)
);

CREATE TABLE IF NOT EXISTS process_request_stakeholder
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    process_request_id BIGINT       NOT NULL,
    stakeholder_id     VARCHAR(100) NOT NULL,
    type               VARCHAR(255) NOT NULL,
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL,
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
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL,
    CONSTRAINT fk_process_event_transition_process FOREIGN KEY (process_id) REFERENCES process (id)
);

CREATE TABLE IF NOT EXISTS transaction
(
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    completed_date           TIMESTAMP    NULL,
    type                     VARCHAR(255) NOT NULL,
    amount                   NUMERIC      NOT NULL,
    fee                      NUMERIC      NOT NULL DEFAULT 0,
    commission               NUMERIC      NOT NULL DEFAULT 0,
    rebate                   NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    currency                 VARCHAR(255) NOT NULL,
    description              VARCHAR(255),
    display_ref              VARCHAR(255) NOT NULL,
    customer_id              VARCHAR(255),
    internal_reference       UUID         NOT NULL,
    external_reference       VARCHAR(255),
    status                   VARCHAR(255),
    channel                  VARCHAR(255),
    sender_account_id        UUID,
    recipient_account_id     UUID,
    sender_running_balance   NUMERIC      DEFAULT 0,
    recipient_running_balance NUMERIC     NOT NULL DEFAULT 0,
    transaction_group        VARCHAR(20)  NOT NULL DEFAULT 'P2P',
    lend                     BOOLEAN      NOT NULL DEFAULT FALSE,
    reconciled               BOOLEAN      NOT NULL DEFAULT FALSE,
    reversed_by              BIGINT,
    reverses                 BIGINT,
    process_id               UUID         NOT NULL,
    created_date             TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by               VARCHAR(255) NOT NULL,
    last_modified_by         VARCHAR(255) NOT NULL,
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ux_transaction_external_reference UNIQUE (external_reference),
    CONSTRAINT ux_transaction_internal_reference UNIQUE (internal_reference),
    CONSTRAINT fk_transaction_reversed_by_id FOREIGN KEY (reversed_by) REFERENCES transaction (id),
    CONSTRAINT fk_transaction_reverses_id FOREIGN KEY (reverses) REFERENCES transaction (id),
    CONSTRAINT fk_transaction_process_id FOREIGN KEY (process_id) REFERENCES process (public_id),
    CONSTRAINT fk_transaction_sender_account FOREIGN KEY (sender_account_id) REFERENCES account(public_id),
    CONSTRAINT fk_transaction_recipient_account FOREIGN KEY (recipient_account_id) REFERENCES account(public_id)
);

CREATE INDEX IF NOT EXISTS idx_transaction_customer_id ON transaction(customer_id);
CREATE INDEX IF NOT EXISTS idx_transaction_sender_account_id ON transaction(sender_account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_recipient_account_id ON transaction(recipient_account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_group ON transaction(transaction_group);
CREATE INDEX IF NOT EXISTS idx_transaction_group_status_created ON transaction(transaction_group, status, created_date);
CREATE INDEX IF NOT EXISTS idx_transaction_group_status_completed ON transaction(transaction_group, status, completed_date);
CREATE INDEX IF NOT EXISTS idx_transaction_lend ON transaction(lend);
CREATE INDEX IF NOT EXISTS idx_transaction_sender_account_id_lend ON transaction(sender_account_id, lend);
CREATE INDEX IF NOT EXISTS idx_transaction_reconciled ON transaction(reconciled);

CREATE TABLE IF NOT EXISTS transaction_property
(
    name               VARCHAR(255),
    value              TEXT,
    transaction_id     BIGINT       NOT NULL,
    property_group     VARCHAR(255),
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL,
    PRIMARY KEY (transaction_id, name),
    CONSTRAINT fk_transaction_property_transaction FOREIGN KEY (transaction_id) REFERENCES transaction (id)
);

CREATE TABLE IF NOT EXISTS transaction_reference_sequences
(
    date_key       DATE      NOT NULL,
    sequence_value INTEGER   NOT NULL,
    created_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (date_key)
);

CREATE INDEX IF NOT EXISTS idx_transaction_reference_sequences_date_key ON transaction_reference_sequences(date_key);

CREATE TABLE IF NOT EXISTS materialized_view_refresh_log (
    id BIGSERIAL PRIMARY KEY,
    view_name VARCHAR(255) NOT NULL,
    refresh_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refresh_completed_at TIMESTAMP,
    refresh_duration_seconds NUMERIC(10,3),
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    error_message TEXT,
    row_count BIGINT
);

CREATE INDEX IF NOT EXISTS idx_mv_refresh_log_view_date ON materialized_view_refresh_log (view_name, refresh_started_at DESC);

CREATE TABLE IF NOT EXISTS billpay_vendor
(
    id                 SERIAL       NOT NULL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    public_id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    currency           VARCHAR(10)  NOT NULL,
    minimum_amount     NUMERIC,
    maximum_amount     NUMERIC,
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ix_vendor_name_unique UNIQUE (name),
    CONSTRAINT chk_billpay_vendor_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX IF NOT EXISTS idx_billpay_vendor_status ON billpay_vendor(status);

CREATE TABLE IF NOT EXISTS billpay_vendor_property
(
    vendor_id INTEGER      NOT NULL,
    name      VARCHAR(255) NOT NULL,
    value     VARCHAR(255) NOT NULL,
    PRIMARY KEY (vendor_id, name),
    FOREIGN KEY (vendor_id) REFERENCES billpay_vendor (id)
);

CREATE TABLE IF NOT EXISTS billpay_product
(
    id                  BIGSERIAL    NOT NULL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    account_description VARCHAR(255) NOT NULL,
    integrator_id       VARCHAR(255) NOT NULL,
    vendor_id           INTEGER      NOT NULL,
    public_id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    type                VARCHAR(255) NOT NULL,
    status              VARCHAR(50)  NOT NULL,
    fixed_price         BOOLEAN      NOT NULL DEFAULT FALSE,
    amount              NUMERIC      NULL,
    currency            VARCHAR(10)  NULL,
    can_lend            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_date        TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255) NOT NULL,
    last_modified_by    VARCHAR(255) NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS unique_active_name_vendor ON billpay_product(name, vendor_id) WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS billpay_product_property
(
    product_id BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    value      VARCHAR(255) NOT NULL,
    PRIMARY KEY (product_id, name),
    FOREIGN KEY (product_id) REFERENCES billpay_product (id)
);

CREATE TABLE IF NOT EXISTS pricing
(
    id                 BIGSERIAL PRIMARY KEY,
    public_id          UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    account_type       VARCHAR(255) NULL,
    account_public_id  UUID,
    transaction_type   VARCHAR(255) NOT NULL,
    integrator_id      VARCHAR(255) NULL,
    product_id         UUID,
    currency           VARCHAR(10),
    valid_from         TIMESTAMP                    DEFAULT NULL,
    valid_until        TIMESTAMP                    DEFAULT NULL,
    created_by         VARCHAR(50)  NOT NULL        DEFAULT 'system',
    created_date       TIMESTAMP                    DEFAULT NOW(),
    last_modified_by   VARCHAR(50)                  DEFAULT NOW(),
    last_modified_date TIMESTAMP                    DEFAULT NOW(),
    version            BIGINT       NOT NULL        DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pricing_data
(
    id                 BIGSERIAL PRIMARY KEY,
    pricing_type       VARCHAR(255) NOT NULL,
    calculation        VARCHAR(255) NOT NULL,
    value              NUMERIC      NOT NULL,
    expression         TEXT                  DEFAULT NULL,
    pricing_id         BIGINT       NOT NULL,
    created_by         VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP             DEFAULT NOW(),
    last_modified_by   VARCHAR(50)           DEFAULT NOW(),
    last_modified_date TIMESTAMP             DEFAULT NOW(),
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_pricing_data_pricing_id FOREIGN KEY (pricing_id) REFERENCES pricing (id)
);

CREATE TABLE IF NOT EXISTS transaction_limit
(
    id                     SERIAL NOT NULL PRIMARY KEY,
    profile_id             UUID,
    transaction_type       VARCHAR(255) NOT NULL,
    account_type           VARCHAR(255) NOT NULL,
    currency               VARCHAR(50)  NOT NULL,
    max_daily_debit        NUMERIC      NOT NULL,
    max_daily_credit       NUMERIC      NOT NULL,
    cumulative_debit       NUMERIC      NOT NULL,
    cumulative_credit      NUMERIC      NOT NULL,
    min_transaction_credit NUMERIC      NOT NULL,
    min_transaction_debit  NUMERIC      NOT NULL,
    max_transaction_debit  NUMERIC      NOT NULL,
    max_transaction_credit NUMERIC      NOT NULL,
    max_account_balance    NUMERIC      NOT NULL,
    start                  TIMESTAMP    NOT NULL,
    expiry                 TIMESTAMP,
    created_by             VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_date           TIMESTAMP             DEFAULT NOW(),
    last_modified_by       VARCHAR(50)           DEFAULT NOW(),
    last_modified_date     TIMESTAMP             DEFAULT NOW(),
    version                BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS access_token
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    type               VARCHAR(255) NOT NULL,
    expiry             TIMESTAMP    NOT NULL,
    access_token       TEXT         NOT NULL,
    refresh_token      TEXT         NULL,
    resource           VARCHAR(255) NOT NULL,
    institution        VARCHAR(255) NOT NULL,
    created_by         VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP             DEFAULT NOW(),
    last_modified_by   VARCHAR(50)           DEFAULT NOW(),
    last_modified_date TIMESTAMP             DEFAULT NOW(),
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS integration_config
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    identifier         VARCHAR(255) NOT NULL,
    priority           INTEGER      NOT NULL,
    action             VARCHAR(255) NOT NULL,
    status             VARCHAR(255) NOT NULL,
    exchange_id        VARCHAR(255) NOT NULL,
    public_id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    start              TIMESTAMP    NOT NULL DEFAULT NOW(),
    expiry             TIMESTAMP,
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_integration_config_identifier_exchange_action UNIQUE (identifier, exchange_id, action)
);

CREATE TABLE IF NOT EXISTS integration_log
(
    id                 BIGSERIAL PRIMARY KEY,
    session_id         TEXT,
    request            TEXT,
    response           TEXT,
    error              TEXT,
    external_reference VARCHAR(255),
    internal_reference UUID,
    inbound            BOOLEAN     NOT NULL,
    created_by         VARCHAR(50) NOT NULL,
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(50),
    last_modified_date TIMESTAMP,
    version            BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS integration_monitoring_log
(
    id                 BIGSERIAL PRIMARY KEY,
    correlation_id     UUID         NOT NULL,
    integrator_id      VARCHAR(255) NOT NULL,
    request_type       VARCHAR(50)  NOT NULL,
    endpoint           VARCHAR(500) NOT NULL,
    method             VARCHAR(10)  NOT NULL,
    request_headers    TEXT,
    request_body       TEXT,
    response_headers   TEXT,
    response_body      TEXT,
    status             VARCHAR(50)  NOT NULL,
    http_status_code   INTEGER,
    response_time      BIGINT       NOT NULL,
    error_code         VARCHAR(100),
    error_message      TEXT,
    external_reference VARCHAR(255),
    internal_reference UUID,
    process_reference  UUID,
    user_id            UUID,
    created_date       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    last_modified_by   VARCHAR(255),
    version            BIGINT    DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_correlation_id ON integration_monitoring_log(correlation_id);
CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_integrator_id ON integration_monitoring_log(integrator_id);
CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_created_date ON integration_monitoring_log(created_date);
CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_status ON integration_monitoring_log(status);
CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_integrator_status_date ON integration_monitoring_log(integrator_id, status, created_date);
CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_integrator_date ON integration_monitoring_log(integrator_id, created_date);
CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_endpoint ON integration_monitoring_log(endpoint);
CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_response_time ON integration_monitoring_log(response_time);
CREATE INDEX IF NOT EXISTS idx_integration_monitoring_log_process_reference ON integration_monitoring_log(process_reference);

CREATE TABLE IF NOT EXISTS integration_downtime_log
(
    id                 BIGSERIAL PRIMARY KEY,
    integrator_id      VARCHAR(255) NOT NULL,
    endpoint           VARCHAR(500) NOT NULL,
    start_time         TIMESTAMP    NOT NULL,
    end_time           TIMESTAMP,
    duration           BIGINT,
    reason             TEXT         NOT NULL,
    is_recovered       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_date       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    last_modified_by   VARCHAR(255),
    version            BIGINT                DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_integration_downtime_log_integrator_endpoint ON integration_downtime_log(integrator_id, endpoint);
CREATE INDEX IF NOT EXISTS idx_integration_downtime_log_is_recovered ON integration_downtime_log(is_recovered);
CREATE INDEX IF NOT EXISTS idx_integration_downtime_log_start_time ON integration_downtime_log(start_time);
CREATE INDEX IF NOT EXISTS idx_integration_downtime_log_integrator_endpoint_recovered ON integration_downtime_log(integrator_id, endpoint, is_recovered);

CREATE TABLE IF NOT EXISTS integration_response_code
(
    id                     BIGSERIAL PRIMARY KEY,
    public_id              UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    integration_name       VARCHAR(100) NOT NULL,
    integrator_code        VARCHAR(50)  NOT NULL,
    internal_response_code VARCHAR(50)  NOT NULL DEFAULT 'UNKNOWN',
    response_description   VARCHAR(500),
    follow_up_action       VARCHAR(50)  NOT NULL,
    is_active              BOOLEAN      NOT NULL        DEFAULT TRUE,
    created_date           TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    last_modified_date     TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by             VARCHAR(100) NOT NULL,
    last_modified_by       VARCHAR(100) NOT NULL,
    version                BIGINT       NOT NULL        DEFAULT 0,
    CONSTRAINT uk_integration_response_code UNIQUE (integration_name, integrator_code)
);

CREATE INDEX IF NOT EXISTS idx_integrator_code_integration ON integration_response_code(integration_name, integrator_code);
CREATE INDEX IF NOT EXISTS idx_response_code_active ON integration_response_code(is_active);

CREATE TABLE IF NOT EXISTS notification_device
(
    id                   BIGSERIAL PRIMARY KEY,
    public_id            UUID        NOT NULL,
    notification_channel VARCHAR(50) NOT NULL,
    value                TEXT        NOT NULL,
    user_id              UUID        NOT NULL,
    notification_type    VARCHAR(50) NOT NULL,
    created_by           VARCHAR(50) NOT NULL default 'system',
    created_date         TIMESTAMP            DEFAULT NOW(),
    last_modified_by     VARCHAR(50)          DEFAULT NOW(),
    last_modified_date   TIMESTAMP            DEFAULT NOW(),
    version              BIGINT      NOT NULL DEFAULT 0,
    UNIQUE (notification_type, notification_channel, user_id)
);

CREATE TABLE IF NOT EXISTS system_property
(
    id                 SERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    scope              VARCHAR(255) NOT NULL,
    value              TEXT         NOT NULL,
    created_by         VARCHAR(50)  NOT NULL,
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(50),
    last_modified_date TIMESTAMP,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT system_property_unique_name_scope UNIQUE (name, scope)
);

CREATE TABLE IF NOT EXISTS users
(
    id                 BIGSERIAL PRIMARY KEY,
    public_id          UUID        NOT NULL UNIQUE,
    created_by         VARCHAR(50) NOT NULL,
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(50),
    last_modified_date TIMESTAMP,
    version            BIGINT      NOT NULL DEFAULT 0
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

CREATE TABLE IF NOT EXISTS loan
(
    id                  BIGSERIAL PRIMARY KEY,
    customer_id         UUID         NOT NULL,
    account_number      VARCHAR(255) NOT NULL,
    internal_reference  VARCHAR(255) NOT NULL,
    external_reference  VARCHAR(255),
    product_id          UUID         NOT NULL,
    public_id           UUID         NOT NULL,
    principal_amount    NUMERIC      NOT NULL,
    service_fee         NUMERIC      NOT NULL,
    commission          NUMERIC      NOT NULL,
    total_amount        NUMERIC      NOT NULL,
    outstanding_balance NUMERIC      NOT NULL,
    currency            VARCHAR(3)   NOT NULL,
    status              VARCHAR(50)  NOT NULL,
    disbursement_date   TIMESTAMP,
    due_date            DATE,
    created_date        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(255),
    last_modified_by    VARCHAR(255),
    version             BIGINT    DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_loan_customer_id ON loan(customer_id);
CREATE INDEX IF NOT EXISTS idx_loan_reference ON loan(internal_reference);
CREATE INDEX IF NOT EXISTS idx_loan_external_reference ON loan(external_reference);
CREATE INDEX IF NOT EXISTS idx_loan_status ON loan(status);
CREATE INDEX IF NOT EXISTS idx_loan_due_date ON loan(due_date);

CREATE TABLE IF NOT EXISTS loan_property
(
    name               VARCHAR(50)   NOT NULL,
    loan_id            BIGINT        NOT NULL,
    value              VARCHAR(1000) NOT NULL,
    property_group     VARCHAR(50)   NOT NULL,
    created_date       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    last_modified_by   VARCHAR(255),
    version            BIGINT    DEFAULT 0,
    PRIMARY KEY (name, loan_id),
    CONSTRAINT fk_loan_property_loan FOREIGN KEY (loan_id) REFERENCES loan (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_loan_property_group ON loan_property(property_group);

CREATE TABLE IF NOT EXISTS loan_repayment
(
    id                    BIGSERIAL PRIMARY KEY,
    loan_id               BIGINT,
    repayment_reference   VARCHAR(255) NOT NULL,
    external_reference    VARCHAR(255),
    public_id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    amount                NUMERIC      NOT NULL,
    currency              VARCHAR(3)   NOT NULL DEFAULT 'NGN',
    type                  VARCHAR(50)  NOT NULL,
    payment_channel       VARCHAR(255),
    transaction_reference VARCHAR(255),
    remarks               VARCHAR(1000),
    created_date          TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    last_modified_date    TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(255),
    last_modified_by      VARCHAR(255),
    version               BIGINT                DEFAULT 0,
    CONSTRAINT fk_loan_repayment_loan FOREIGN KEY (loan_id) REFERENCES loan (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_loan_repayment_loan_id ON loan_repayment(loan_id);
CREATE INDEX IF NOT EXISTS idx_loan_repayment_reference ON loan_repayment(repayment_reference);
CREATE INDEX IF NOT EXISTS idx_loan_repayment_external_reference ON loan_repayment(external_reference);
CREATE INDEX IF NOT EXISTS idx_loan_repayment_type ON loan_repayment(type);

CREATE TABLE IF NOT EXISTS loan_repayment_property
(
    name               VARCHAR(50)   NOT NULL,
    repayment_id       BIGINT        NOT NULL,
    value              VARCHAR(1000) NOT NULL,
    property_group     VARCHAR(50)   NOT NULL,
    created_date       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    last_modified_by   VARCHAR(255),
    version            BIGINT    DEFAULT 0,
    PRIMARY KEY (name, repayment_id),
    CONSTRAINT fk_loan_repayment_property_repayment FOREIGN KEY (repayment_id) REFERENCES loan_repayment (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_loan_repayment_property_group ON loan_repayment_property(property_group);

CREATE TABLE IF NOT EXISTS support_ticket
(
    id                 BIGSERIAL PRIMARY KEY,
    public_id          UUID         NOT NULL,
    external_ticket_id VARCHAR(255),
    subject            VARCHAR(500) NOT NULL,
    description        TEXT         NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    priority           VARCHAR(20)  NOT NULL,
    category           VARCHAR(30)  NOT NULL,
    customer_email     VARCHAR(255) NOT NULL,
    customer_name      VARCHAR(255),
    assignee_id        VARCHAR(255),
    web_url            VARCHAR(500),
    closed_date        TIMESTAMP,
    process_id         UUID         NOT NULL,
    vendor_id          UUID,
    merchant_id        UUID,
    transaction_id     UUID,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_date       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255)          DEFAULT 'system',
    last_modified_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(255)          DEFAULT 'system'
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_support_ticket_public_id ON support_ticket(public_id);
CREATE INDEX IF NOT EXISTS idx_support_ticket_external_id ON support_ticket(external_ticket_id);
CREATE INDEX IF NOT EXISTS idx_support_ticket_status ON support_ticket(status);
CREATE INDEX IF NOT EXISTS idx_support_ticket_customer_email ON support_ticket(customer_email);
CREATE INDEX IF NOT EXISTS idx_support_ticket_process_id ON support_ticket(process_id);
CREATE INDEX IF NOT EXISTS idx_support_ticket_created_date ON support_ticket(created_date);
CREATE INDEX IF NOT EXISTS idx_support_ticket_vendor_id ON support_ticket(vendor_id);
CREATE INDEX IF NOT EXISTS idx_support_ticket_merchant_id ON support_ticket(merchant_id);
CREATE INDEX IF NOT EXISTS idx_support_ticket_transaction_id ON support_ticket(transaction_id);

CREATE TABLE IF NOT EXISTS webhook_configuration
(
    id                 BIGSERIAL PRIMARY KEY,
    public_id          UUID         NOT NULL UNIQUE,
    merchant_id        UUID         NOT NULL,
    webhook_url        VARCHAR(2048) NOT NULL,
    notification_type  VARCHAR(50)  NOT NULL,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    secret_key         VARCHAR(512),
    custom_headers     TEXT,
    retry_attempts     INTEGER      NOT NULL DEFAULT 3,
    timeout_seconds    INTEGER      NOT NULL DEFAULT 30,
    description        TEXT,
    created_date       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255)          DEFAULT 'system',
    last_modified_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(255)          DEFAULT 'system',
    version            BIGINT       NOT NULL DEFAULT 0
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
    created_date             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_delivery_log_configuration FOREIGN KEY (webhook_configuration_id) REFERENCES webhook_configuration(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_config_id ON webhook_delivery_log(webhook_configuration_id);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_event_id ON webhook_delivery_log(event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_status ON webhook_delivery_log(delivery_status);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_created_date ON webhook_delivery_log(created_date);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_log_config_status_date ON webhook_delivery_log(webhook_configuration_id, delivery_status, created_date);

CREATE TABLE IF NOT EXISTS vendor_integration_config
(
    id                 SERIAL PRIMARY KEY,
    public_id          UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    integration_id     VARCHAR(255)             NOT NULL,
    vendor_name        VARCHAR(255)             NOT NULL,
    pool_account_id    UUID                     NOT NULL,
    currency_code      VARCHAR(3)               NOT NULL        DEFAULT 'NGN',
    is_active          BOOLEAN                  NOT NULL        DEFAULT TRUE,
    version            BIGINT                   NOT NULL        DEFAULT 0,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255)             NOT NULL        DEFAULT 'system',
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(255)             NOT NULL        DEFAULT 'system',
    CONSTRAINT unique_integration_currency UNIQUE (integration_id, currency_code)
);

CREATE INDEX IF NOT EXISTS idx_vendor_integration_config_pool_account ON vendor_integration_config(pool_account_id);
CREATE INDEX IF NOT EXISTS idx_vendor_integration_config_integration_id ON vendor_integration_config(integration_id);
CREATE INDEX IF NOT EXISTS idx_vendor_integration_config_active ON vendor_integration_config(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_vendor_integration_config_public_id ON vendor_integration_config(public_id);

CREATE TABLE IF NOT EXISTS reconciliation_record
(
    id                    BIGSERIAL PRIMARY KEY,
    transaction_id        UUID         NOT NULL UNIQUE,
    external_reference    VARCHAR(255),
    display_ref           VARCHAR(255) NOT NULL,
    transaction_type      VARCHAR(50)  NOT NULL,
    integrator            VARCHAR(100),
    retry_count           INTEGER      NOT NULL DEFAULT 0,
    first_failed_date     TIMESTAMP,
    last_retry_date       TIMESTAMP,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reconciliation_action VARCHAR(50),
    reconciled_by         UUID,
    reconciled_date       TIMESTAMP,
    notes                 TEXT,
    target_reference      VARCHAR(255),
    process_reference     UUID,
    error_message         TEXT,
    created_date          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(255),
    last_modified_by      VARCHAR(255),
    version               BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_record_transaction_id ON reconciliation_record(transaction_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_record_status ON reconciliation_record(status);
CREATE INDEX IF NOT EXISTS idx_reconciliation_record_integrator ON reconciliation_record(integrator);
CREATE INDEX IF NOT EXISTS idx_reconciliation_record_transaction_type ON reconciliation_record(transaction_type);
CREATE INDEX IF NOT EXISTS idx_reconciliation_record_created_date ON reconciliation_record(created_date);
CREATE INDEX IF NOT EXISTS idx_reconciliation_record_external_reference ON reconciliation_record(external_reference);
CREATE INDEX IF NOT EXISTS idx_reconciliation_record_process_reference ON reconciliation_record(process_reference);
CREATE INDEX IF NOT EXISTS idx_reconciliation_record_status_process_null ON reconciliation_record(status, process_reference) WHERE process_reference IS NULL;

CREATE TABLE IF NOT EXISTS message_template
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    channel            VARCHAR(255) NOT NULL,
    content            TEXT         NOT NULL,
    title              VARCHAR(255) NOT NULL,
    name               VARCHAR(255) NOT NULL,
    locale             VARCHAR(10)  NOT NULL,
    external_id        VARCHAR(255),
    active             BOOLEAN,
    recipient_type     VARCHAR(100) NOT NULL,
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS message
(
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    channel            VARCHAR(255) NOT NULL,
    recipient          VARCHAR(255) NOT NULL,
    template_id        BIGINT       NOT NULL,
    delivery_status    VARCHAR(255),
    request            TEXT         NOT NULL,
    response           TEXT,
    integrator         VARCHAR(100) NOT NULL,
    priority           VARCHAR(20)  NOT NULL,
    client_identifier  VARCHAR(255) NOT NULL UNIQUE,
    locale             VARCHAR(10)  NOT NULL,
    delivery_date      TIMESTAMP,
    public_id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    status             VARCHAR(20)  NOT NULL,
    sent_message_id    VARCHAR(25),
    created_date       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_message_template_message FOREIGN KEY (template_id) REFERENCES message_template (id)
);

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
