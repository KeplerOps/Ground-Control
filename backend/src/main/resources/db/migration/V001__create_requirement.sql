CREATE TABLE requirement (
    id              UUID PRIMARY KEY,
    uid             VARCHAR(50)  NOT NULL UNIQUE,
    title           VARCHAR(255) NOT NULL,
    statement       TEXT         NOT NULL,
    rationale       TEXT         DEFAULT '',
    requirement_type VARCHAR(20) NOT NULL DEFAULT 'FUNCTIONAL',
    priority        VARCHAR(10)  NOT NULL DEFAULT 'MUST',
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    wave            INTEGER,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    archived_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_requirement_uid ON requirement(uid);
CREATE INDEX idx_requirement_status ON requirement(status);
CREATE INDEX idx_requirement_archived_at ON requirement(archived_at);
