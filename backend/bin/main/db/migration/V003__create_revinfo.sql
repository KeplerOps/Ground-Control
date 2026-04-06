CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE revinfo (
    rev        INTEGER      NOT NULL DEFAULT nextval('revinfo_seq') PRIMARY KEY,
    revtstmp   BIGINT       NOT NULL
);
