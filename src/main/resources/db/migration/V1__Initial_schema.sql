-- Flyway placeholder for initial schema migration
-- In production, generate this script using:
-- mvn hibernate-schema-export:export
-- Or use the Hibernate DDL generation

-- This file ensures Flyway has a baseline for schema versioning
-- The actual schema will be created by Hibernate when ddl-auto=validate is used

-- For PostgreSQL production deployment:
-- 1. Run the application with ddl-auto=create first
-- 2. Export the generated schema
-- 3. Replace this file with the exported schema
-- 4. Change ddl-auto to validate

SELECT 1; -- Placeholder query