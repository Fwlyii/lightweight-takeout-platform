CREATE TABLE ai_chat_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, session_id VARCHAR(64),
 user_message TEXT, ai_response TEXT, chat_type VARCHAR(20), processing_time BIGINT, context_data TEXT,
 create_time TIMESTAMP, creator BIGINT, is_deleted TINYINT DEFAULT 0,
 update_time TIMESTAMP, updater BIGINT
);
