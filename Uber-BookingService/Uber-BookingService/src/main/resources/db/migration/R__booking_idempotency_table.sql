CREATE TABLE IF NOT EXISTS booking_idempotency
(
    passenger_id     BIGINT       NOT NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    booking_id       BIGINT       NOT NULL,
    created_at       DATETIME     NOT NULL,
    CONSTRAINT pk_booking_idempotency PRIMARY KEY (passenger_id, idempotency_key),
    CONSTRAINT uq_booking_idempotency_booking UNIQUE (booking_id),
    CONSTRAINT fk_booking_idempotency_booking FOREIGN KEY (booking_id) REFERENCES booking (id)
);
