-- Define a shared merchant_id for all non-admin users
DO $$
DECLARE
    shared_merchant_id UUID := gen_random_uuid();
BEGIN
    -- Insert fake data for regular user
    UPDATE identity.oauth_users 
    SET 
        first_name = 'Umoh',
        last_name = 'Bassey-Duke',
        phone_number = '+2348095231000',
        merchant_id = shared_merchant_id,
        aku_id = gen_random_uuid(),
        user_type = 'INDIVIDUAL',
        trust_level = 'VERIFIED',
        email_verified = false,
        phone_number_verified = true,
        date_of_birth = '1990-01-15',
        tax_identification_number = 'TIN123456789',
        locale = 'en'
    WHERE username = 'user';

    -- Update admin user with different fake data and unique merchant_id
    UPDATE identity.oauth_users 
    SET 
        first_name = 'Admin',
        last_name = 'User',
        phone_number = '+2348012345678',
        merchant_id = gen_random_uuid(),
        aku_id = gen_random_uuid(),
        user_type = 'BUSINESS',
        trust_level = 'PREMIUM',
        email_verified = true,
        phone_number_verified = true,
        date_of_birth = '1985-06-20',
        tax_identification_number = 'TIN987654321',
        locale = 'en'
    WHERE username = 'admin';

    -- Update any other existing non-admin users with shared merchant_id
    UPDATE identity.oauth_users 
    SET 
        first_name = COALESCE(first_name, 'John'),
        last_name = COALESCE(last_name, 'Doe'),
        phone_number = COALESCE(phone_number, '+2348000000000'),
        merchant_id = COALESCE(merchant_id, shared_merchant_id),
        aku_id = COALESCE(aku_id, gen_random_uuid()),
        user_type = COALESCE(user_type, 'INDIVIDUAL'),
        trust_level = COALESCE(trust_level, 'BASIC'),
        date_of_birth = COALESCE(date_of_birth, '1990-01-01'),
        tax_identification_number = COALESCE(tax_identification_number, 'TIN000000000'),
        locale = COALESCE(locale, 'en')
    WHERE username != 'admin' AND (first_name IS NULL OR last_name IS NULL);
END $$;