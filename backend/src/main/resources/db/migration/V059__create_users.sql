-- ADR-037 / GC-P017: Spring Security default user / authority schema for the
-- browser session login chain. The column names and types here match the
-- defaults baked into Spring Security's JdbcUserDetailsManager (see
-- JdbcDaoImpl#DEF_USERS_BY_USERNAME_QUERY and #DEF_AUTHORITIES_BY_USERNAME_QUERY)
-- so the framework can read and write the tables without a custom query
-- override. Diverging from those defaults would force us to maintain a parallel
-- query set in BrowserSecurityConfig; keeping the names stock is the lower-risk
-- choice for a security boundary.
--
-- These tables hold *security principals*, not domain entities. ADR-037 §4 is
-- the source of truth: no project membership, no groups, no tenants, no
-- federation metadata, no profile fields. If a future change needs richer
-- user-lifecycle data, that is the seam for a dedicated domain model and a new
-- ADR — not extra columns here.

CREATE TABLE users (
    username VARCHAR(255) NOT NULL PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    enabled  BOOLEAN      NOT NULL,
    CONSTRAINT users_username_format CHECK (
        username = lower(username)
        AND char_length(username) BETWEEN 2 AND 64
        AND username ~ '^[a-z][a-z0-9._-]{1,63}$'
    )
);

CREATE TABLE authorities (
    username  VARCHAR(255) NOT NULL,
    authority VARCHAR(64)  NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username)
        REFERENCES users (username) ON DELETE CASCADE,
    CONSTRAINT authorities_user_authority_unique UNIQUE (username, authority),
    CONSTRAINT authorities_role_vocabulary CHECK (authority IN ('ROLE_USER', 'ROLE_ADMIN'))
);

CREATE INDEX idx_authorities_username ON authorities (username);
