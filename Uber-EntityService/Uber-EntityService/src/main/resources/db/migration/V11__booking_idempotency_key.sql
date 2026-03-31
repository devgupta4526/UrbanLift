ALTER TABLE booking
    ADD COLUMN idempotency_key VARCHAR(128) NULL;

CREATE UNIQUE INDEX uq_booking_passenger_idempotency
    ON booking (passenger_id, idempotency_key);

CREATE TABLE IF NOT EXISTS trip_rating
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    booking_id     BIGINT NOT NULL,
    direction      VARCHAR(32) NOT NULL,
    from_user_id   BIGINT NOT NULL,
    to_user_id     BIGINT NOT NULL,
    score          INT NOT NULL,
    comment        VARCHAR(500) NULL,
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME NOT NULL,
    CONSTRAINT pk_trip_rating PRIMARY KEY (id),
    CONSTRAINT uq_trip_rating_booking_direction UNIQUE (booking_id, direction),
    CONSTRAINT fk_trip_rating_booking FOREIGN KEY (booking_id) REFERENCES booking (id)
    );

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