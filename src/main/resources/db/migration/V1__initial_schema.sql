-- ============================================================
-- CRM - ESQUEMA INICIAL
-- PostgreSQL
--
-- Módulos:
-- 1. Dashboard
-- 2. Leads
-- 3. Clientes
-- 4. Oportunidades
-- 5. Pipeline
-- 6. Actividades
-- 7. Usuarios
-- ============================================================


-- ============================================================
-- FUNCIÓN GENÉRICA PARA updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- 1. ROLES
-- ============================================================

CREATE TABLE roles (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    name VARCHAR(30) NOT NULL UNIQUE,

    description VARCHAR(150),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 2. USUARIOS
-- ============================================================

CREATE TABLE app_users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    first_name VARCHAR(80) NOT NULL,

    last_name VARCHAR(80) NOT NULL,

    email VARCHAR(150) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- Email único ignorando mayúsculas/minúsculas
CREATE UNIQUE INDEX ux_app_users_email_lower
    ON app_users (LOWER(email));


CREATE INDEX idx_app_users_enabled
    ON app_users(enabled);


CREATE TRIGGER trg_app_users_updated_at
BEFORE UPDATE ON app_users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- 3. RELACIÓN USUARIOS - ROLES
-- ============================================================

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,

    role_id SMALLINT NOT NULL,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);


CREATE INDEX idx_user_roles_role
    ON user_roles(role_id);


-- ============================================================
-- ROLES INICIALES
-- ============================================================

INSERT INTO roles (name, description)
VALUES
    ('ADMIN', 'Acceso completo al CRM'),
    ('MANAGER', 'Gestión comercial y supervisión'),
    ('SALES', 'Gestión de leads, clientes y oportunidades');


-- ============================================================
-- 4. LEADS
-- ============================================================

CREATE TABLE leads (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100),

    company_name VARCHAR(150),

    email VARCHAR(150),

    phone VARCHAR(30),

    source VARCHAR(30) NOT NULL DEFAULT 'OTHER',

    status VARCHAR(30) NOT NULL DEFAULT 'NEW',

    notes TEXT,

    assigned_to_id BIGINT,

    created_by_id BIGINT,

    converted_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_lead_source CHECK (
        source IN (
            'WEBSITE',
            'FACEBOOK',
            'INSTAGRAM',
            'WHATSAPP',
            'REFERRAL',
            'PHONE',
            'EMAIL',
            'OTHER'
        )
    ),

    CONSTRAINT chk_lead_status CHECK (
        status IN (
            'NEW',
            'CONTACTED',
            'QUALIFIED',
            'DISQUALIFIED',
            'CONVERTED'
        )
    ),

    CONSTRAINT fk_lead_assigned_user
        FOREIGN KEY (assigned_to_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_lead_created_by
        FOREIGN KEY (created_by_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL
);


CREATE INDEX idx_leads_status
    ON leads(status);


CREATE INDEX idx_leads_source
    ON leads(source);


CREATE INDEX idx_leads_assigned_to
    ON leads(assigned_to_id);


CREATE INDEX idx_leads_created_at
    ON leads(created_at);


CREATE INDEX idx_leads_email_lower
    ON leads(LOWER(email));


CREATE INDEX idx_leads_phone
    ON leads(phone);


CREATE TRIGGER trg_leads_updated_at
BEFORE UPDATE ON leads
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- 5. CLIENTES
-- ============================================================

CREATE TABLE clients (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    client_type VARCHAR(20) NOT NULL DEFAULT 'PERSON',

    first_name VARCHAR(100),

    last_name VARCHAR(100),

    company_name VARCHAR(150),

    document_number VARCHAR(30),

    email VARCHAR(150),

    phone VARCHAR(30),

    address VARCHAR(255),

    source_lead_id BIGINT UNIQUE,

    assigned_to_id BIGINT,

    created_by_id BIGINT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_client_type CHECK (
        client_type IN ('PERSON', 'COMPANY')
    ),

    CONSTRAINT chk_client_required_name CHECK (
        (
            client_type = 'PERSON'
            AND first_name IS NOT NULL
        )
        OR
        (
            client_type = 'COMPANY'
            AND company_name IS NOT NULL
        )
    ),

    CONSTRAINT fk_client_source_lead
        FOREIGN KEY (source_lead_id)
        REFERENCES leads(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_client_assigned_user
        FOREIGN KEY (assigned_to_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_client_created_by
        FOREIGN KEY (created_by_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL
);


CREATE UNIQUE INDEX ux_clients_document_number
    ON clients(document_number)
    WHERE document_number IS NOT NULL;


CREATE INDEX idx_clients_email_lower
    ON clients(LOWER(email));


CREATE INDEX idx_clients_phone
    ON clients(phone);


CREATE INDEX idx_clients_assigned_to
    ON clients(assigned_to_id);


CREATE INDEX idx_clients_active
    ON clients(active);


CREATE INDEX idx_clients_created_at
    ON clients(created_at);


CREATE TRIGGER trg_clients_updated_at
BEFORE UPDATE ON clients
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- 6. ETAPAS DEL PIPELINE
-- ============================================================

CREATE TABLE pipeline_stages (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    code VARCHAR(40) NOT NULL UNIQUE,

    name VARCHAR(80) NOT NULL,

    display_order SMALLINT NOT NULL,

    is_closed BOOLEAN NOT NULL DEFAULT FALSE,

    is_won BOOLEAN NOT NULL DEFAULT FALSE,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ux_pipeline_stage_order
        UNIQUE(display_order),

    CONSTRAINT chk_pipeline_won_closed
        CHECK (
            is_won = FALSE
            OR is_closed = TRUE
        )
);


CREATE TRIGGER trg_pipeline_stages_updated_at
BEFORE UPDATE ON pipeline_stages
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- ETAPAS INICIALES
-- ============================================================

INSERT INTO pipeline_stages (
    code,
    name,
    display_order,
    is_closed,
    is_won
)
VALUES
    (
        'PROSPECTING',
        'Prospección',
        1,
        FALSE,
        FALSE
    ),
    (
        'QUALIFICATION',
        'Calificación',
        2,
        FALSE,
        FALSE
    ),
    (
        'PROPOSAL',
        'Propuesta',
        3,
        FALSE,
        FALSE
    ),
    (
        'NEGOTIATION',
        'Negociación',
        4,
        FALSE,
        FALSE
    ),
    (
        'WON',
        'Ganada',
        5,
        TRUE,
        TRUE
    ),
    (
        'LOST',
        'Perdida',
        6,
        TRUE,
        FALSE
    );


-- ============================================================
-- 7. OPORTUNIDADES
-- ============================================================

CREATE TABLE opportunities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    name VARCHAR(180) NOT NULL,

    description TEXT,

    client_id BIGINT NOT NULL,

    owner_id BIGINT,

    created_by_id BIGINT,

    stage_id SMALLINT NOT NULL,

    amount NUMERIC(14,2) NOT NULL DEFAULT 0,

    probability SMALLINT NOT NULL DEFAULT 0,

    expected_close_date DATE,

    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',

    lost_reason TEXT,

    closed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_opportunity_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_opportunity_probability
        CHECK (
            probability >= 0
            AND probability <= 100
        ),

    CONSTRAINT chk_opportunity_status
        CHECK (
            status IN (
                'OPEN',
                'WON',
                'LOST'
            )
        ),

    CONSTRAINT fk_opportunity_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_opportunity_owner
        FOREIGN KEY (owner_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_opportunity_created_by
        FOREIGN KEY (created_by_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_opportunity_stage
        FOREIGN KEY (stage_id)
        REFERENCES pipeline_stages(id)
        ON DELETE RESTRICT
);


CREATE INDEX idx_opportunities_client
    ON opportunities(client_id);


CREATE INDEX idx_opportunities_owner
    ON opportunities(owner_id);


CREATE INDEX idx_opportunities_stage
    ON opportunities(stage_id);


CREATE INDEX idx_opportunities_status
    ON opportunities(status);


CREATE INDEX idx_opportunities_expected_close
    ON opportunities(expected_close_date);


CREATE INDEX idx_opportunities_created_at
    ON opportunities(created_at);


CREATE TRIGGER trg_opportunities_updated_at
BEFORE UPDATE ON opportunities
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- 8. HISTORIAL DEL PIPELINE
-- ============================================================

CREATE TABLE opportunity_stage_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    opportunity_id BIGINT NOT NULL,

    from_stage_id SMALLINT,

    to_stage_id SMALLINT NOT NULL,

    changed_by_id BIGINT,

    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stage_history_opportunity
        FOREIGN KEY (opportunity_id)
        REFERENCES opportunities(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_stage_history_from
        FOREIGN KEY (from_stage_id)
        REFERENCES pipeline_stages(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_stage_history_to
        FOREIGN KEY (to_stage_id)
        REFERENCES pipeline_stages(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_stage_history_user
        FOREIGN KEY (changed_by_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL
);


CREATE INDEX idx_stage_history_opportunity
    ON opportunity_stage_history(opportunity_id);


CREATE INDEX idx_stage_history_changed_at
    ON opportunity_stage_history(changed_at);


-- ============================================================
-- 9. ACTIVIDADES
-- ============================================================

CREATE TABLE activities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    activity_type VARCHAR(30) NOT NULL,

    subject VARCHAR(180) NOT NULL,

    description TEXT,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',

    scheduled_at TIMESTAMPTZ,

    due_at TIMESTAMPTZ,

    completed_at TIMESTAMPTZ,

    owner_id BIGINT,

    created_by_id BIGINT,

    lead_id BIGINT,

    client_id BIGINT,

    opportunity_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_activity_type CHECK (
        activity_type IN (
            'TASK',
            'CALL',
            'MEETING',
            'FOLLOW_UP'
        )
    ),

    CONSTRAINT chk_activity_status CHECK (
        status IN (
            'PENDING',
            'IN_PROGRESS',
            'COMPLETED',
            'CANCELLED'
        )
    ),

    CONSTRAINT chk_activity_priority CHECK (
        priority IN (
            'LOW',
            'MEDIUM',
            'HIGH'
        )
    ),

    -- Una actividad puede estar asociada a un Lead,
    -- Cliente u Oportunidad.
    -- También puede ser una actividad general sin relación.
    CONSTRAINT chk_activity_single_relation CHECK (
        num_nonnulls(
            lead_id,
            client_id,
            opportunity_id
        ) <= 1
    ),

    CONSTRAINT fk_activity_owner
        FOREIGN KEY (owner_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_activity_created_by
        FOREIGN KEY (created_by_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_activity_lead
        FOREIGN KEY (lead_id)
        REFERENCES leads(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_activity_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_activity_opportunity
        FOREIGN KEY (opportunity_id)
        REFERENCES opportunities(id)
        ON DELETE SET NULL
);


CREATE INDEX idx_activities_owner
    ON activities(owner_id);


CREATE INDEX idx_activities_lead
    ON activities(lead_id);


CREATE INDEX idx_activities_client
    ON activities(client_id);


CREATE INDEX idx_activities_opportunity
    ON activities(opportunity_id);


CREATE INDEX idx_activities_status
    ON activities(status);


CREATE INDEX idx_activities_due_at
    ON activities(due_at);


CREATE INDEX idx_activities_pending_due
    ON activities(due_at)
    WHERE status IN ('PENDING', 'IN_PROGRESS');


CREATE TRIGGER trg_activities_updated_at
BEFORE UPDATE ON activities
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
