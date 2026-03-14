-- Apache AGE graph setup (optional — succeeds on plain PostgreSQL too)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS age;
    LOAD 'age';
    SET search_path = ag_catalog, "$user", public;
    PERFORM create_graph('requirements');
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Apache AGE not available — skipping graph setup: %', SQLERRM;
END $$;
