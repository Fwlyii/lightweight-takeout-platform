-- Test-only projection of the production tables used by profile/address/favorites.
-- is_default is the proposed address migration exercised by this feature.
CREATE TABLE delivery_address (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id),
 contact_name VARCHAR(255), contact_sex INT, contact_tel VARCHAR(255), address VARCHAR(255),
 creator BIGINT, updater BIGINT, create_time TIMESTAMP, update_time TIMESTAMP,
 is_deleted TINYINT DEFAULT 0, is_default TINYINT NOT NULL DEFAULT 0
);
CREATE TABLE business (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, business_name VARCHAR(255),
 business_address VARCHAR(255), business_explain VARCHAR(255), business_img TEXT,
 start_price DECIMAL(10,2), delivery_price DECIMAL(10,2), order_type_id INT,
 status INT DEFAULT 1, operating_status TINYINT DEFAULT 1, is_deleted TINYINT DEFAULT 0,
 demo_rating DECIMAL(3,2), demo_sales_count INT DEFAULT 0, create_time TIMESTAMP,
 dine_in_available TINYINT, promotion_threshold DECIMAL(10,2), promotion_discount DECIMAL(10,2)
);
CREATE TABLE merchant_interaction (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id),
 merchant_id BIGINT NOT NULL REFERENCES business(id), liked TINYINT DEFAULT 0,
 collected TINYINT DEFAULT 0, create_time TIMESTAMP, update_time TIMESTAMP,
 CONSTRAINT uk_user_merchant UNIQUE(user_id, merchant_id)
);
CREATE TABLE review (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, business_id BIGINT, rating INT, is_hidden TINYINT DEFAULT 0
);
CREATE TABLE orders (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, business_id BIGINT, order_state INT,
 order_total DECIMAL(10,2), is_deleted TINYINT DEFAULT 0, order_date TIMESTAMP
);
