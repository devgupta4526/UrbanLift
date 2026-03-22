-- V7__add_wallet_payment_fare_config.sql

CREATE TABLE wallet
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    created_at  DATETIME              NOT NULL,
    updated_at  DATETIME              NOT NULL,
    user_id     BIGINT                NULL,
    user_type   ENUM ('PASSENGER','DRIVER') NULL,
    balance     DECIMAL(19, 2)        NULL,
    currency    VARCHAR(10)           NULL,
    CONSTRAINT pk_wallet PRIMARY KEY (id)
);

CREATE TABLE payment
(
    id                  BIGINT AUTO_INCREMENT NOT NULL,
    created_at          DATETIME              NOT NULL,
    updated_at          DATETIME              NOT NULL,
    booking_id          BIGINT                NULL,
    amount              DECIMAL(19, 2)        NULL,
    status              ENUM ('PENDING','COMPLETED','FAILED','REFUNDED') NULL,
    gateway_order_id    VARCHAR(255)          NULL,
    gateway_payment_id  VARCHAR(255)          NULL,
    CONSTRAINT pk_payment PRIMARY KEY (id)
);

CREATE TABLE fare_config
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    created_at        DATETIME       NOT NULL,
    updated_at        DATETIME       NOT NULL,
    car_type          VARCHAR(50)    NULL,
    base_fare         DECIMAL(19, 2) NULL,
    per_km_rate       DECIMAL(19, 2) NULL,
    per_min_rate      DECIMAL(19, 2) NULL,
    surge_multiplier  DECIMAL(5, 2)  NULL,
    CONSTRAINT pk_fare_config PRIMARY KEY (id)
);

ALTER TABLE payment
    ADD CONSTRAINT FK_PAYMENT_ON_BOOKING FOREIGN KEY (booking_id) REFERENCES booking (id);

ALTER TABLE wallet
    ADD CONSTRAINT UQ_WALLET_USER UNIQUE (user_id, user_type);