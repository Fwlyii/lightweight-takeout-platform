-- MySQL 8.0. Run against the selected application database before deploying the backend.
-- Historical names cannot be reconstructed: leave old rows NULL, never copy today's menu.
SET @snapshot_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orderdetailet'
      AND column_name = 'food_name_snapshot'
);
SET @snapshot_ddl = IF(@snapshot_column_exists = 0,
    'ALTER TABLE orderdetailet ADD COLUMN food_name_snapshot VARCHAR(100) NULL DEFAULT NULL COMMENT ''下单时商品名称，历史记录不回填''',
    'SELECT 1');
PREPARE snapshot_statement FROM @snapshot_ddl;
EXECUTE snapshot_statement;
DEALLOCATE PREPARE snapshot_statement;
