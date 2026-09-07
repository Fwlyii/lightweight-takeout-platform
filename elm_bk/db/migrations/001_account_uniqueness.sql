-- Existing databases only. Fresh databases already have these indexes in elm_v2.sql.
-- Run once after resolving duplicates; do not delete or rewrite account records automatically.
-- Keep identifiers reserved after soft deletion so old accounts cannot be impersonated.
SELECT username, COUNT(*) AS occurrences FROM users GROUP BY username HAVING COUNT(*) > 1;
SELECT phone, COUNT(*) AS occurrences FROM person WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*) > 1;

ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
ALTER TABLE person ADD CONSTRAINT uk_person_phone UNIQUE (phone);
