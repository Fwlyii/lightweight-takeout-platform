-- Authentication subset of elm_v2.sql; isolated in-memory database for each test run.
-- Do not strengthen constraints here beyond the production schema.
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    create_time TIMESTAMP,
    creator BIGINT,
    is_deleted TINYINT,
    update_time TIMESTAMP,
    updater BIGINT,
    activated TINYINT NOT NULL,
    password VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL
);
CREATE TABLE IF NOT EXISTS person (
    id BIGINT PRIMARY KEY REFERENCES users(id),
    email VARCHAR(255),
    first_name VARCHAR(255),
    gender VARCHAR(255),
    last_name VARCHAR(255),
    phone VARCHAR(255),
    photo TEXT
);
CREATE TABLE IF NOT EXISTS authority (name VARCHAR(50) PRIMARY KEY);
CREATE TABLE IF NOT EXISTS user_authority (
    user_id BIGINT REFERENCES users(id),
    authority_name VARCHAR(50) REFERENCES authority(name),
    PRIMARY KEY (user_id, authority_name)
);
