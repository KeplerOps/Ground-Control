CREATE TABLE github_pr_sync (
    id              UUID PRIMARY KEY,
    pr_number       INTEGER UNIQUE NOT NULL,
    pr_title        VARCHAR(500) NOT NULL,
    pr_state        VARCHAR(10)  NOT NULL,
    pr_url          VARCHAR(2000) NOT NULL,
    pr_body         TEXT DEFAULT '',
    base_branch     VARCHAR(255) DEFAULT '',
    head_branch     VARCHAR(255) DEFAULT '',
    pr_labels       JSONB DEFAULT '[]'::jsonb,
    last_fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
