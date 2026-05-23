-- ============================================================
-- V1 — Initial Schema
-- ============================================================

-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id                          CHAR(36)        NOT NULL PRIMARY KEY,

    full_name                   VARCHAR(255)    NOT NULL,

    email                       VARCHAR(255)    NOT NULL UNIQUE,
    phone_number                VARCHAR(20)     NOT NULL UNIQUE,

    password                    VARCHAR(255)    NOT NULL,

    bvn_nin                     VARCHAR(255),

    account_status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    otp_action                  VARCHAR(30)     NOT NULL DEFAULT 'EMAIL_VERIFICATION',
    role                        VARCHAR(20)     NOT NULL DEFAULT 'USER',

    otp                         VARCHAR(255),
    otp_expiry                  DATETIME,

    otp_attempt_count           INT             NOT NULL DEFAULT 0,

    password_reset_token        VARCHAR(255),

    last_login                  DATETIME,

    email_verified              TINYINT(1)      NOT NULL DEFAULT 0,

    created_date                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- WALLETS
-- ============================================================

CREATE TABLE IF NOT EXISTS wallets (
    wallet_id                       CHAR(36)        NOT NULL PRIMARY KEY,

    user_id                         CHAR(36)        NOT NULL UNIQUE,

    wallet_balance_encrypted        TEXT            NOT NULL,
    wallet_balance_hash             VARCHAR(255),

    currency                        VARCHAR(3)      NOT NULL DEFAULT 'NGN',

    wallet_status                   VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    created_date                    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version                         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT fk_wallet_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ============================================================
-- LOANS
-- ============================================================

CREATE TABLE IF NOT EXISTS loans (
    loan_id                            CHAR(36)        NOT NULL PRIMARY KEY,

    user_id                            CHAR(36)        NOT NULL,

    loan_amount_encrypted              TEXT            NOT NULL,

    interest_rate                      DECIMAL(5, 2)   NOT NULL,

    loan_duration_days                 INT             NOT NULL,

    total_repayment_encrypted          TEXT,

    amount_repaid_encrypted            TEXT,

    remaining_balance_encrypted        TEXT,

    loan_integrity_hash                VARCHAR(255),

    loan_status                        VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    repayment_schedule                 JSON,

    repayment_due_date                 DATE,

    due_date                           DATETIME,

    approved_at                        DATETIME,

    disbursed_at                       DATETIME,

    created_date                       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_loan_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ============================================================
-- TRANSACTIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id                 CHAR(36)        NOT NULL PRIMARY KEY,

    user_id                        CHAR(36)        NOT NULL,

    wallet_id                      CHAR(36)        NOT NULL,

    transaction_type               VARCHAR(30)     NOT NULL,

    amount                         DECIMAL(15, 2)  NOT NULL,

    transaction_status             VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    reference_number               VARCHAR(100)    NOT NULL UNIQUE,

    external_reference             VARCHAR(100),

    description                    VARCHAR(500),

    timestamp                      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tx_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_tx_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id)
);

-- ============================================================
-- TOKEN BLACKLIST
-- ============================================================

CREATE TABLE IF NOT EXISTS token_blacklist (
    id                      BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,

    token                   VARCHAR(512)  NOT NULL,

    blacklisted_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    expires_at              DATETIME       NOT NULL,

    CONSTRAINT uk_blacklisted_token
        UNIQUE (token)
);

-- ============================================================
-- AUDIT LOGS
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id                      BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,

    user_id                 VARCHAR(255),

    action                  VARCHAR(255),

    entity_type             VARCHAR(255),

    entity_id               VARCHAR(255),

    request_payload         JSON,

    response_payload        JSON,

    ip_address              VARCHAR(255),

    status                  VARCHAR(50),

    created_date            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_loans_user_id
ON loans(user_id);

CREATE INDEX idx_loans_status
ON loans(loan_status);

CREATE INDEX idx_loans_due_date
ON loans(due_date);

CREATE INDEX idx_tx_user_id
ON transactions(user_id);

CREATE INDEX idx_tx_wallet_id
ON transactions(wallet_id);

CREATE INDEX idx_tx_reference
ON transactions(reference_number);

CREATE INDEX idx_tx_timestamp
ON transactions(timestamp);

CREATE INDEX idx_audit_created_date
ON audit_logs(created_date);