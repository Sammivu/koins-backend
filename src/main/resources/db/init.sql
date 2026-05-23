CREATE DATABASE IF NOT EXISTS koins_db;
USE koins_db;

-- Sample admin user (password: Admin@12345)
-- Note: Run the app first to let Hibernate create the tables, then run this insert
-- INSERT INTO users (id, full_name, email, phone_number, password, account_status, role, created_date, updated_date)
-- VALUES (UUID(), 'System Admin', 'admin@koins.com', '+2348000000001',
--   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', -- Admin@12345
--   'ACTIVE', 'ADMIN', NOW(), NOW());
