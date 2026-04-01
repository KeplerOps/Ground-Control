CREATE TABLE asset_link (
    id                UUID                     PRIMARY KEY,
    asset_id          UUID                     NOT NULL REFERENCES operational_asset(id) ON DELETE CASCADE,
    target_type       VARCHAR(20)              NOT NULL,
    target_identifier VARCHAR(500)             NOT NULL,
    link_type         VARCHAR(20)              NOT NULL,
    target_url        VARCHAR(2000)            DEFAULT '',
    target_title      VARCHAR(255)             DEFAULT '',
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (asset_id, target_type, target_identifier, link_type)
);

CREATE INDEX idx_asset_link_asset_id ON asset_link(asset_id);
CREATE INDEX idx_asset_link_target ON asset_link(target_type, target_identifier);
CREATE INDEX idx_asset_link_type ON asset_link(link_type);
