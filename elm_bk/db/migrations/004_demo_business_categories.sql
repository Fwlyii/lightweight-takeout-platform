-- Apply to the selected database only after backing up business.
-- Correct only the original demo IDs/names/incorrect categories; reruns are safe.
-- Never rerun demo-seed.sql against a live database to correct these records.
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE business SET order_type_id = 6
 WHERE id = 1 AND business_name = '北洋食堂·现炒' AND order_type_id = 4 AND is_deleted = 0;
UPDATE business SET order_type_id = 7
 WHERE id = 4 AND business_name = '津南麻辣香锅' AND order_type_id = 10 AND is_deleted = 0;
UPDATE business SET order_type_id = 5
 WHERE id = 5 AND business_name = '北洋咖啡实验室' AND order_type_id = 2 AND is_deleted = 0;
UPDATE business SET order_type_id = 8
 WHERE id = 6 AND business_name = '清真兰州牛肉面' AND order_type_id = 4 AND is_deleted = 0;
UPDATE business SET order_type_id = 6
 WHERE id = 7 AND business_name = '轻食研究所' AND order_type_id = 4 AND is_deleted = 0;
COMMIT;
