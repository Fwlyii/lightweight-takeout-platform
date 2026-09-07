-- Isolated H2 equivalents of rider_profile and notification in elm_v2.sql.
CREATE TABLE IF NOT EXISTS rider_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    audit_status TINYINT NOT NULL DEFAULT 0,
    online TINYINT NOT NULL DEFAULT 0,
    reject_reason VARCHAR(255),
    completed_orders INT NOT NULL DEFAULT 0,
    total_distance DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_income DECIMAL(10,2) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type TINYINT NOT NULL,
    notification_content VARCHAR(500) NOT NULL,
    audit_result TINYINT NOT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    read_time TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
