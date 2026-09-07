-- Existing MySQL database only. Back up first; execute once before starting this version.
-- Fresh databases use elm_v2.sql instead. No addresses are deleted or reassigned.
ALTER TABLE delivery_address ADD COLUMN is_default TINYINT(1) NOT NULL DEFAULT 0;

-- Assign the oldest active address to accounts that already have addresses.
UPDATE delivery_address a
JOIN (
    SELECT user_id, MIN(id) AS first_id FROM delivery_address
    WHERE is_deleted = 0 GROUP BY user_id
) selected ON selected.user_id = a.user_id AND selected.first_id = a.id
SET a.is_default = 1;
