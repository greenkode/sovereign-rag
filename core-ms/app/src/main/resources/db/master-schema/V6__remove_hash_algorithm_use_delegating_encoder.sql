-- V6: Remove hash_algorithm column and use DelegatingPasswordEncoder prefix format
--
-- Spring Security's DelegatingPasswordEncoder stores the algorithm as a prefix in the hash itself.
-- Format: {algorithm}encodedHash
-- Example: {bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
--
-- This migration:
-- 1. Updates existing BCrypt hashes to add the {bcrypt} prefix
-- 2. Drops the hash_algorithm column (no longer needed)

-- Step 1: Add {bcrypt} prefix to all existing api_key_hash values that don't already have it
-- Only update if the hash doesn't already start with {bcrypt}
UPDATE master.tenants
SET api_key_hash = '{bcrypt}' || api_key_hash
WHERE api_key_hash NOT LIKE '{%';

-- Step 2: Drop the hash_algorithm column
ALTER TABLE master.tenants
DROP COLUMN IF EXISTS hash_algorithm;
