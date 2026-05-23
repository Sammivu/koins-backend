-- ============================================================
-- V2 — Seed admin user
-- Password: Admin@123 (BCrypt encoded)
-- ============================================================
INSERT INTO users (id, full_name, email, phone_number, password, account_status, otp_action, role, email_verified)
VALUES (
    UUID(),
    'KOINS Admin',
    'admin@koins.com',
    '08000000000',
    '$2a$12$R6.6ikvqOiaNR2SEpsW/QuMLtGX9CjcyzjT6vyNL/Ywgalo0bXdJS',
    'ACTIVE',
    'EMAIL_VERIFICATION',
    'ADMIN',
    1
)
ON DUPLICATE KEY UPDATE email = email;
