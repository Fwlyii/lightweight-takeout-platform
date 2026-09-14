CREATE TABLE review (
 id BIGINT PRIMARY KEY, business_id BIGINT, rating INT, is_hidden TINYINT DEFAULT 0
);
CREATE TABLE user_preference (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT UNIQUE, theme VARCHAR(20), spicy_level INT,
 taste_tags VARCHAR(255), avoid_tags VARCHAR(255), category_tags VARCHAR(255), update_time TIMESTAMP
);
