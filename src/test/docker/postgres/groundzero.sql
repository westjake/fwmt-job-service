DO $$
BEGIN
IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='jobservicerole') THEN
   REVOKE ALL PRIVILEGES ON DATABASE postgres FROM jobservicerole;
END IF;
END$$;

DROP SCHEMA IF EXISTS jobservice CASCADE;
DROP ROLE IF EXISTS jobservicerole;

CREATE ROLE jobservicerole PASSWORD 'jobservicerole' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION INHERIT LOGIN;

CREATE SCHEMA jobservice AUTHORIZATION jobservicerole;

REVOKE ALL ON ALL TABLES IN SCHEMA jobservice FROM PUBLIC;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA jobservice FROM PUBLIC;
REVOKE CONNECT ON DATABASE postgres FROM PUBLIC;

GRANT CONNECT ON DATABASE postgres TO jobservicerole;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA jobservice TO jobservicerole;
GRANT ALL ON ALL SEQUENCES IN SCHEMA jobservice TO jobservicerole;
