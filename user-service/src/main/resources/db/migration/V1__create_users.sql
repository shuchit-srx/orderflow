CREATE TABLE users (
                       id UUID PRIMARY KEY,

                       email VARCHAR(320) NOT NULL UNIQUE,

                       password_hash VARCHAR(255) NOT NULL,

                       display_name VARCHAR(120) NOT NULL,

                       role VARCHAR(20) NOT NULL,

                       status VARCHAR(20) NOT NULL,

                       created_at TIMESTAMPTZ NOT NULL,

                       updated_at TIMESTAMPTZ NOT NULL,

                       CONSTRAINT chk_users_email_normalized
                           CHECK (email = LOWER(email)),

                       CONSTRAINT chk_users_role
                           CHECK (role IN ('CUSTOMER', 'ADMIN')),

                       CONSTRAINT chk_users_status
                           CHECK (status IN ('ACTIVE', 'DISABLED'))
);