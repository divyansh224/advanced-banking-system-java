-- ================================================================
-- Online Banking System - MySQL Schema
-- (Reference DDL - Hibernate also auto-generates/updates this
--  schema at startup via spring.jpa.hibernate.ddl-auto=update)
-- ================================================================

CREATE DATABASE IF NOT EXISTS bankingdb;
USE bankingdb;

-- ---------------------------------------------------------------
-- users
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(120) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(20),
    role            VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------
-- accounts
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number  VARCHAR(20)  NOT NULL UNIQUE,
    user_id         BIGINT       NOT NULL,
    account_type    VARCHAR(20)  NOT NULL DEFAULT 'SAVINGS',
    balance         DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT       DEFAULT 0,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);

-- ---------------------------------------------------------------
-- transactions
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_ref   VARCHAR(40)  NOT NULL UNIQUE,
    from_account_id   BIGINT,
    to_account_id     BIGINT,
    amount            DECIMAL(19,2) NOT NULL,
    type              VARCHAR(20)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    description       VARCHAR(255),
    timestamp         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_from_account FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_txn_to_account   FOREIGN KEY (to_account_id)   REFERENCES accounts(id),
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_txn_from_account ON transactions(from_account_id);
CREATE INDEX idx_txn_to_account   ON transactions(to_account_id);
CREATE INDEX idx_accounts_user    ON accounts(user_id);

-- ---------------------------------------------------------------
-- fraud_analysis
-- One row per transaction, produced by the rule-based FraudRiskEngine
-- BEFORE any balance is touched. HIGH/CRITICAL risk transfers are held as
-- a PENDING transaction with no money moved until an admin reviews them.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fraud_analysis (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  BIGINT       NOT NULL UNIQUE,
    risk_score      INT          NOT NULL,
    risk_level      VARCHAR(20)  NOT NULL,  -- LOW / MEDIUM / HIGH / CRITICAL
    fraud_flag      BOOLEAN      NOT NULL DEFAULT FALSE,
    fraud_reason    VARCHAR(500),
    review_status   VARCHAR(20)  NOT NULL DEFAULT 'NOT_FLAGGED', -- NOT_FLAGGED / PENDING_REVIEW / APPROVED / REJECTED
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     DATETIME,
    reviewed_by     VARCHAR(50),
    CONSTRAINT fk_fraud_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT chk_risk_score_range CHECK (risk_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_fraud_review_status ON fraud_analysis(review_status);
