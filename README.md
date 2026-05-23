# KOINS — Secure Fintech Loan & Wallet Management System

A production-ready **Spring Boot fintech backend** implementing:

- User onboarding & authentication
- OTP + email verification
- Wallet funding & payment verification
- Secure wallet balance encryption
- Loan processing & repayment tracking
- Financial tamper detection
- Admin approval/disbursement workflow
- Audit logging
- Asynchronous notifications with RabbitMQ
- Scheduled loan reminders/defaulting

---

# Tech Stack

| Layer | Technology |
|--------|-------------|
| Framework | Java 17, Spring Boot 3 |
| Security | Spring Security 6, JWT |
| Database | MySQL 8, Spring Data JPA / Hibernate |
| Migration | Flyway |
| Payment | Paystack |
| Messaging | RabbitMQ |
| Email | JavaMailSender (SMTP) |
| API Docs | Swagger / OpenAPI |
| Containerization | Docker + Docker Compose |

---

# Architecture Overview

KOINS follows a layered architecture:

```text
Controller Layer
        ↓
Service Layer
        ↓
Repository Layer
        ↓
MySQL Database
```

# Support Services
````text
RabbitMQ → Async notifications
Paystack → Wallet funding
Flyway → DB migration versioning
JWT → Authentication
CryptoUtil → Financial integrity verification
Scheduler → Loan reminders/defaulting
````

# Project Structure

````integrationperformancetest
src/main/java/com/koins/
│
├── config/                  # OpenAPI, Security, RabbitMQ configs
│
├── controller/              # REST Controllers
│   ├── AuthController
│   ├── WalletController
│   ├── LoanController
│   ├── TransactionController
│   └── WebhookController
│
├── dto/
│   ├── request/
│   └── response/
│
├── entity/
│   ├── User
│   ├── Wallet
│   ├── Loan
│   ├── Transaction
│   ├── AuditLog
│   └── TokenBlacklist
│
├── repository/
│
├── scheduler/
│   └── LoanScheduler
│
├── security/
│   ├── JwtAuthenticationFilter
│   ├── SecurityConfig
│   ├── CryptoUtil
│   └── CustomUserDetailsService
│
├── service/
│   └── impl/
│
├── messaging/
│   ├── NotificationPublisher
│   └── consumers/
│
├── exception/
│
└── util/
    ├── JwtUtil
    └── Util
    └── BalanceEncryptionConverter
````
# Core Features
# 1. Authentication & Authorization

Features:

User registration
Login with JWT
Role-based access control
Email verification
OTP verification
Forgot password
Token blacklist logout

````integrationperformancetest
| Role  | Permissions                                         |
| ----- | --------------------------------------------------- |
| USER  | Wallet funding, loan application, repayment         |
| ADMIN | Approve/disburse loans, manual payment verification |
````

# 2. OTP & Email Verification

KOINS uses OTP-based verification.

OTP Actions
EMAIL_VERIFICATION
PASSWORD_RESET
Rules
OTP expires after 5 minutes
OTP resend supported
Email must be verified before login
Failed attempts tracked

# 3. Wallet Management

Wallets are automatically created after registration.

Features
Wallet funding
Wallet balance retrieval
Transaction history
Paystack integration
Manual payment verification (Admin)
Security

Wallet balances are encrypted at rest.

Sensitive balance fields are stored as:
````integrationperformancetest
wallet_balance_encrypted
wallet_balance_hash
````
Balances are validated using HMAC signatures to detect tampering.

Example validation:
````text
walletId|userId|balance
````
If tampering is detected:
````
Wallet balance tampering detected
````
# 4. Loan Management

Users can apply for loans based on wallet balance.

Business Rules
* Wallet must be funded
* Maximum loan amount = 3x wallet balance
* Only approved loans can be disbursed
* Only disbursed loans can be repaid
* Loan repayment reduces remaining balance
* Fully repaid loans become REPAID

Loan States:
````integrationperformancetest
PENDING
APPROVED
DISBURSED
REPAID
DEFAULTED
REJECTED
````
Loan Security

Loan financial fields are encrypted.

Stored fields:
```integrationperformancetest
loan_amount_encrypted
total_repayment_encrypted
remaining_balance_encrypted
amount_repaid_encrypted
```
Integrity is verified using:
```integrationperformancetest
loan_integrity_hash
```
Tampering protection includes:
```text
loanId|userId|loanAmount
|totalRepayment
|amountRepaid
|remainingBalance
|loanStatus
```
If validation fails:
```text
Loan data tampering detected
```

# Payment Flow
Wallet Funding
````java
User initiates payment
        ↓
Paystack payment initialized
        ↓
Webhook confirmation
        ↓
Wallet credited
        ↓
Transaction created
        ↓
Notification sent
````
**Manual Verification (Admin)**

Admin can manually verify payment.

This:

* Credits wallet
* Regenerates wallet hash
* Marks transaction successful
* Publishes notification


# Loan Flow
**Loan Application**
```java
User applies
        ↓
Wallet balance checked
        ↓
Repayment schedule generated
        ↓
Loan created
        ↓
Loan hash generated
        ↓
Notification sent
```

**Loan Approval**
```java
Admin approves loan
        ↓
Status = APPROVED
        ↓
Hash regenerated
```

**Loan Disbursement**
```java
Admin disburses loan
        ↓
Wallet credited
        ↓
Transaction created
        ↓
Status = DISBURSED
        ↓
Hash regenerated
```
**Loan Repayment**
```java
User repays
        ↓
Wallet debited
        ↓
Transaction created
        ↓
Remaining balance updated
        ↓
Repayment installment updated
        ↓
Hash regenerated
```

# Scheduler Jobs
**1. Overdue Loan Detection**

Runs daily at midnight
```java
0 0 0 * * *
```
Behavior:

* Finds overdue disbursed loans
* Marks them DEFAULTED
* Regenerates loan hash
* Sends notification

**2. Repayment Reminder Emails**

Runs daily at 9 AM
````java
0 0 9 * * *
````
Behavior:

* Finds loans due in 3 days
* Sends reminder email

# RabbitMQ Notifications

Notifications are asynchronous.

**Events**
* Authentication
* Registration OTP
* Password reset OTP
* Email verification

**Wallet**
* Wallet funded
* Payment verified

**Loan**
* Loan applied
* Loan approved
* Loan disbursed
* Loan repaid
* Loan defaulted
# Audit Logging

Critical actions are recorded.

Stored fields:
```text
user_id
action
entity_type
entity_id
request_payload
response_payload
ip_address
status
created_date
```
#  API Endpoints
Authentication

Base URL: [{{BaseUrl}}/api/v1/auth]()

```toml
| Method | Endpoint         | Description       |
| ------ | ---------------- | ----------------- |
| POST   | /register        | Register user     |
| POST   | /verify-email    | Verify email OTP  |
| POST   | /resend-otp      | Resend OTP        |
| POST   | /login           | Login             |
| POST   | /logout          | Logout            |
| POST   | /forgot-password | Request reset OTP |
| POST   | /verify-otp      | Verify OTP        |
| POST   | /reset-password  | Reset password    |

```

#  Wallet

Base URL: /api/v1/wallet
````toml
| Method | Endpoint        | Description                 |
| ------ | --------------- | --------------------------- |
| GET    | /balance        | Get balance                 |
| POST   | /fund           | Initialize funding          |
| POST   | /verify-payment | Manual payment verification |
| GET    | /transactions   | Wallet history              |
````
#  Loans

Base URL: /api/v1/loans
```toml
| Method | Endpoint       | Access |
| ------ | -------------- | ------ |
| POST   | /apply         | USER   |
| PUT    | /{id}/approve  | ADMIN  |
| PUT    | /{id}/disburse | ADMIN  |
| POST   | /{id}/repay    | USER   |
| GET    | /{id}          | USER   |
| GET    | /              | USER   |
| GET    | /all           | ADMIN  |
```

#  Transactions

Base URL: /api/v1/transactions

```toml
| Method | Endpoint |
| ------ | -------- |
| GET    | /        |
| GET    | /{id}    |
```

#  Webhooks

Base URL: /api/v1/webhooks
```toml
| Method | Endpoint  |
| ------ | --------- |
| POST   | /paystack |

```
Webhook signature is validated using:
```text
x-paystack-signature
```
#  Database Migrations

Managed using Flyway.

Migration naming:
```text
V1__initial_schema.sql
V2__seed_admin_user.sql
```

#  Environment Variables
```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=koins_db
DB_USERNAME=root
DB_PASSWORD=password

JWT_SECRET=your_secret
JWT_EXPIRATION_MS=86400000

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=app_password

PAYSTACK_SECRET_KEY=sk_test_xxx

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

CRYPTO_SECRET=super_secret_key
```
#  Running Locally
```text
App: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui/index.html

Admin User
Seeded automatically via Flyway.

Credentials:
Email: admin@koins.com
Password: Admin@123
```


# Docker

```text
START APP: docker-compose up --build

STOP: docker-compose down
```

# Security Features
* JWT Authentication
* BCrypt password hashing
* Token blacklist logout
* Wallet balance encryption
* Loan amount encryption
* HMAC tamper detection
* Role-based authorization
* Paystack signature verification
