-- Additive migration: no guessed geocoding, no changes to existing addresses/orders.
ALTER TABLE business
 ADD COLUMN longitude DECIMAL(10,6) NULL,
 ADD COLUMN latitude DECIMAL(10,6) NULL,
 ADD COLUMN poi_id VARCHAR(80) NULL,
 ADD COLUMN adcode VARCHAR(6) NULL,
 ADD COLUMN formatted_address VARCHAR(255) NULL;
ALTER TABLE delivery_address
 ADD COLUMN longitude DECIMAL(10,6) NULL,
 ADD COLUMN latitude DECIMAL(10,6) NULL,
 ADD COLUMN poi_id VARCHAR(80) NULL,
 ADD COLUMN adcode VARCHAR(6) NULL,
 ADD COLUMN formatted_address VARCHAR(255) NULL;
