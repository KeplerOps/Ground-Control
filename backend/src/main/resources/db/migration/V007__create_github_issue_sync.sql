CREATE TABLE github_issue_sync (
    id               UUID PRIMARY KEY,
    issue_number     INTEGER      NOT NULL UNIQUE,
    issue_title      VARCHAR(500) NOT NULL,
    issue_state      VARCHAR(10)  NOT NULL,
    issue_url        VARCHAR(2000) NOT NULL,
    issue_body       TEXT         DEFAULT '',
    phase            INTEGER,
    priority_label   VARCHAR(10)  DEFAULT '',
    issue_labels     JSONB        DEFAULT '[]'::jsonb,
    cross_references JSONB        DEFAULT '[]'::jsonb,
    last_fetched_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_github_issue_sync_issue_number ON github_issue_sync(issue_number);
CREATE INDEX idx_github_issue_sync_issue_state ON github_issue_sync(issue_state);
