-- =====================================================================
-- V5: Rework the owner account into a shop profile.
--   - shop_name  : the shop's name; also the LOGIN identifier (unique)
--   - owner_name : the person who owns/runs the shop
--   - phone/email/upi_id : set later on the profile page (nullable)
-- The existing 'username' column stays as the unique login key; we keep it
-- in sync with the (normalized) shop name.
-- =====================================================================

ALTER TABLE app_user ADD COLUMN shop_name   VARCHAR(120);
ALTER TABLE app_user ADD COLUMN owner_name  VARCHAR(120);
ALTER TABLE app_user ADD COLUMN phone       VARCHAR(20);
ALTER TABLE app_user ADD COLUMN email       VARCHAR(160);
ALTER TABLE app_user ADD COLUMN upi_id      VARCHAR(80);

-- Backfill existing rows so NOT NULL can be applied (uses current display/username).
UPDATE app_user SET shop_name  = COALESCE(display_name, username) WHERE shop_name  IS NULL;
UPDATE app_user SET owner_name = COALESCE(display_name, username) WHERE owner_name IS NULL;

ALTER TABLE app_user ALTER COLUMN shop_name  SET NOT NULL;
ALTER TABLE app_user ALTER COLUMN owner_name SET NOT NULL;
