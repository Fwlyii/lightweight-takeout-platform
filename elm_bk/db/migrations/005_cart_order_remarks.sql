-- Idempotent MySQL 8 migration.
SET @remarks_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'cart' AND column_name = 'remarks');
SET @remarks_ddl = IF(@remarks_exists = 0, 'ALTER TABLE cart ADD COLUMN remarks VARCHAR(255) NULL DEFAULT NULL', 'SELECT 1');
PREPARE remarks_statement FROM @remarks_ddl;
EXECUTE remarks_statement;
DEALLOCATE PREPARE remarks_statement;
SET @remarks_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'remarks');
SET @remarks_ddl = IF(@remarks_exists = 0, 'ALTER TABLE orders ADD COLUMN remarks VARCHAR(255) NULL DEFAULT NULL', 'SELECT 1');
PREPARE remarks_statement FROM @remarks_ddl;
EXECUTE remarks_statement;
DEALLOCATE PREPARE remarks_statement;
