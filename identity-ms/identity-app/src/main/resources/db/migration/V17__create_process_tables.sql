CREATE TABLE IF NOT EXISTS process (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    state VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    expiry TIMESTAMP NOT NULL,
    external_reference VARCHAR(255),
    integrator_reference VARCHAR(255),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    version bigint not null default 0
);

CREATE INDEX IF NOT EXISTS idx_process_public_id ON process(public_id);
CREATE INDEX IF NOT EXISTS idx_process_type ON process(type);
CREATE INDEX IF NOT EXISTS idx_process_state ON process(state);
CREATE INDEX IF NOT EXISTS idx_process_external_reference ON process(external_reference);
CREATE INDEX IF NOT EXISTS idx_process_expiry ON process(expiry);
CREATE INDEX IF NOT EXISTS idx_process_type_state ON process(type, state);
CREATE INDEX IF NOT EXISTS idx_process_external_ref_state ON process(external_reference, state);
CREATE INDEX IF NOT EXISTS idx_process_created_date ON process(created_date);

CREATE TABLE IF NOT EXISTS process_request (
    id BIGSERIAL PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES process(id),
    user_id UUID NOT NULL,
    type VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    version bigint not null default 0
);

CREATE INDEX IF NOT EXISTS idx_process_request_process_id ON process_request(process_id);
CREATE INDEX IF NOT EXISTS idx_process_request_user_id ON process_request(user_id);
CREATE INDEX IF NOT EXISTS idx_process_request_type ON process_request(type);
CREATE INDEX IF NOT EXISTS idx_process_request_state ON process_request(state);

CREATE TABLE IF NOT EXISTS process_request_data (
    process_request_id BIGINT NOT NULL REFERENCES process_request(id),
    name VARCHAR(255) NOT NULL,
    value TEXT NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    version bigint not null default 0,
    PRIMARY KEY (process_request_id, name)
);

CREATE INDEX IF NOT EXISTS idx_process_request_data_request_id ON process_request_data(process_request_id);
CREATE INDEX IF NOT EXISTS idx_process_request_data_name ON process_request_data(name);

CREATE TABLE IF NOT EXISTS process_request_stakeholder (
    id BIGSERIAL PRIMARY KEY,
    process_request_id BIGINT NOT NULL REFERENCES process_request(id),
    stakeholder_id VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    version bigint not null default 0
);

CREATE INDEX IF NOT EXISTS idx_process_stakeholder_request_id ON process_request_stakeholder(process_request_id);
CREATE INDEX IF NOT EXISTS idx_process_stakeholder_stakeholder_id ON process_request_stakeholder(stakeholder_id);
CREATE INDEX IF NOT EXISTS idx_process_stakeholder_type ON process_request_stakeholder(type);

CREATE TABLE IF NOT EXISTS process_event_transition_entity (
    id BIGSERIAL PRIMARY KEY,
    process_id BIGINT NOT NULL REFERENCES process(id),
    event VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    old_state VARCHAR(50) NOT NULL,
    new_state VARCHAR(50) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    version bigint not null default 0
);

CREATE INDEX IF NOT EXISTS idx_process_transition_process_id ON process_event_transition_entity(process_id);
CREATE INDEX IF NOT EXISTS idx_process_transition_event ON process_event_transition_entity(event);
CREATE INDEX IF NOT EXISTS idx_process_transition_user_id ON process_event_transition_entity(user_id);
CREATE INDEX IF NOT EXISTS idx_process_transition_states ON process_event_transition_entity(old_state, new_state);
CREATE INDEX IF NOT EXISTS idx_process_transition_created_date ON process_event_transition_entity(created_date);