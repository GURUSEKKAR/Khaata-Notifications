-- =====================================================================
-- V4: Scope customers to the owner (app_user) who created them, so each
-- shopkeeper only sees their own customers. Previously the customer table
-- was global and shared across all logins.
-- =====================================================================

ALTER TABLE customer ADD COLUMN owner_id BIGINT;

-- Backfill any pre-existing customers to the earliest owner account so they
-- are not orphaned. (New installs have no rows, so this is a no-op there.)
UPDATE customer
SET owner_id = (SELECT id FROM app_user ORDER BY id ASC LIMIT 1)
WHERE owner_id IS NULL;

-- From now on every customer must belong to an owner.
ALTER TABLE customer ALTER COLUMN owner_id SET NOT NULL;

ALTER TABLE customer
  ADD CONSTRAINT fk_customer_owner FOREIGN KEY (owner_id) REFERENCES app_user(id);

-- Phone must be unique PER OWNER, not globally (two shops may both have a
-- customer with the same phone). Drop the old global unique, add a composite one.
ALTER TABLE customer DROP CONSTRAINT IF EXISTS uq_customer_phone;
-- Some engines name the implicit unique constraint differently; guard with IF EXISTS.
ALTER TABLE customer ADD CONSTRAINT uq_customer_owner_phone UNIQUE (owner_id, phone);

CREATE INDEX idx_customer_owner ON customer (owner_id);
