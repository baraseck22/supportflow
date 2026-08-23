CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'SUPPORT_N1', 'SUPPORT_N2', 'ADMIN'))
);

CREATE TABLE category (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_category_name UNIQUE (name)
);

CREATE TABLE ticket (
    id UUID PRIMARY KEY,
    ticket_number VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL,
    assigned_to UUID,
    category_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_ticket_number UNIQUE (ticket_number),
    CONSTRAINT ck_ticket_status CHECK (status IN ('NEW', 'IN_PROGRESS', 'WAITING', 'ESCALATED', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_ticket_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT fk_ticket_created_by FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT fk_ticket_assigned_to FOREIGN KEY (assigned_to) REFERENCES app_user (id),
    CONSTRAINT fk_ticket_category FOREIGN KEY (category_id) REFERENCES category (id)
);

CREATE TABLE ticket_comment (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_comment_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES app_user (id)
);

CREATE TABLE ticket_history (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    changed_by UUID NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_history_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_changed_by FOREIGN KEY (changed_by) REFERENCES app_user (id)
);

CREATE INDEX idx_ticket_status ON ticket (status);
CREATE INDEX idx_ticket_priority ON ticket (priority);
CREATE INDEX idx_ticket_created_by ON ticket (created_by);
CREATE INDEX idx_ticket_assigned_to ON ticket (assigned_to);
CREATE INDEX idx_ticket_created_at ON ticket (created_at);
CREATE INDEX idx_comment_ticket ON ticket_comment (ticket_id);
CREATE INDEX idx_history_ticket ON ticket_history (ticket_id);
