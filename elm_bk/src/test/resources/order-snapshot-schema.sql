-- 测试专用表：默认在 H2 中快速回归，同一套测试也会连接独立 MySQL 验证。
CREATE TABLE business (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, business_name VARCHAR(255) NOT NULL,
 business_address VARCHAR(255), business_explain VARCHAR(255), business_img TEXT,
 start_price DECIMAL(10,2), delivery_price DECIMAL(10,2), order_type_id INT, remarks VARCHAR(255),
 status INT DEFAULT 1, operating_status TINYINT DEFAULT 1, is_deleted TINYINT DEFAULT 0,
 dine_in_available TINYINT DEFAULT 1, promotion_threshold DECIMAL(10,2), promotion_discount DECIMAL(10,2),
 demo_rating DECIMAL(3,2), demo_sales_count INT, create_time TIMESTAMP, update_time TIMESTAMP,
 creator BIGINT, updater BIGINT
);
CREATE TABLE food (
 id BIGINT PRIMARY KEY, business_id BIGINT NOT NULL REFERENCES business(id),
 food_name VARCHAR(255) NOT NULL, food_price DECIMAL(10,2) NOT NULL,
 food_explain VARCHAR(255), food_img TEXT, remarks VARCHAR(255), category VARCHAR(32),
 stock INT NOT NULL DEFAULT 100, purchase_limit INT, shelve_status INT DEFAULT 1,
 is_deleted TINYINT DEFAULT 0, create_time TIMESTAMP, update_time TIMESTAMP, creator BIGINT, updater BIGINT
);
CREATE TABLE cart (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_id BIGINT NOT NULL REFERENCES users(id),
 business_id BIGINT NOT NULL REFERENCES business(id), food_id BIGINT NOT NULL REFERENCES food(id),
 quantity INT, is_deleted TINYINT DEFAULT 0, create_time TIMESTAMP, update_time TIMESTAMP,
 creator BIGINT, updater BIGINT
);
CREATE TABLE delivery_address (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), address VARCHAR(255),
 contact_name VARCHAR(255), contact_sex INT, contact_tel VARCHAR(255), is_default TINYINT DEFAULT 0,
 is_deleted TINYINT DEFAULT 0, create_time TIMESTAMP, update_time TIMESTAMP, creator BIGINT, updater BIGINT
);
CREATE TABLE orders (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, business_id BIGINT NOT NULL REFERENCES business(id),
 customer_id BIGINT NOT NULL REFERENCES users(id), address_id BIGINT REFERENCES delivery_address(id),
 order_state INT, order_total DECIMAL(10,2) NOT NULL, delivery_price DECIMAL(10,2) NOT NULL,
 order_date TIMESTAMP, create_time TIMESTAMP, update_time TIMESTAMP, creator BIGINT, updater BIGINT,
 is_deleted TINYINT DEFAULT 0, service_mode VARCHAR(16) DEFAULT 'DELIVERY',
 payment_method VARCHAR(20) DEFAULT 'SIMULATED', payment_status VARCHAR(20) DEFAULT 'PENDING',
 points_used INT DEFAULT 0, wallet_paid TINYINT DEFAULT 0, idempotency_key VARCHAR(64),
 address_snapshot VARCHAR(255), contact_name_snapshot VARCHAR(80), contact_sex_snapshot INT,
 contact_tel_snapshot VARCHAR(30), UNIQUE(customer_id, idempotency_key)
);
CREATE TABLE orderdetailet (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES orders(id),
 food_id BIGINT NOT NULL REFERENCES food(id), quantity INT, food_price DECIMAL(10,2) NOT NULL,
 -- 本轮目标结构；旧业务代码不写这个字段，RED 会验证出名称未保存。
 food_name_snapshot VARCHAR(255), is_deleted TINYINT DEFAULT 0,
 create_time TIMESTAMP, update_time TIMESTAMP, creator BIGINT, updater BIGINT
);
CREATE TABLE order_status_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES orders(id),
 from_status INT, to_status INT NOT NULL, operator_user_id BIGINT, reason VARCHAR(255), create_time TIMESTAMP
);
CREATE TABLE user_asset (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
 balance DECIMAL(10,2) NOT NULL DEFAULT 0, points INT NOT NULL DEFAULT 0,
 membership_expire TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE user_coupon (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), name VARCHAR(80),
 discount_amount DECIMAL(10,2), min_order_amount DECIMAL(10,2), expires_at TIMESTAMP,
 used TINYINT DEFAULT 0, order_id BIGINT, create_time TIMESTAMP
);
CREATE TABLE user_asset_ledger (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), type VARCHAR(32),
 amount DECIMAL(10,2), points_delta INT, reason VARCHAR(255), reference_id BIGINT, create_time TIMESTAMP
);
CREATE TABLE notification (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, notification_type INT,
 notification_content VARCHAR(500), audit_result INT, is_read INT DEFAULT 0,
 create_time TIMESTAMP, read_time TIMESTAMP, is_deleted INT DEFAULT 0
);
