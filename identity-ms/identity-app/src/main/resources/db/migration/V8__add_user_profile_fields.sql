-- Add additional user profile fields to support JWT token structure
ALTER TABLE identity.oauth_users 
ADD COLUMN first_name VARCHAR(100),
ADD COLUMN middle_name VARCHAR(100),
ADD COLUMN last_name VARCHAR(100),
ADD COLUMN phone_number VARCHAR(20),
ADD COLUMN merchant_id UUID,
ADD COLUMN aku_id UUID,
ADD COLUMN user_type VARCHAR(20),
ADD COLUMN trust_level VARCHAR(20),
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN phone_number_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN date_of_birth DATE,
ADD COLUMN tax_identification_number VARCHAR(50),
ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'en';