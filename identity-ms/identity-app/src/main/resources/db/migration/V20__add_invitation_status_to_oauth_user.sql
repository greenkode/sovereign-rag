ALTER TABLE oauth_users
ADD COLUMN invitation_status BOOLEAN DEFAULT FALSE;

UPDATE oauth_users
SET invitation_status = CASE
    WHEN email_verified = TRUE THEN TRUE
    ELSE FALSE
END;