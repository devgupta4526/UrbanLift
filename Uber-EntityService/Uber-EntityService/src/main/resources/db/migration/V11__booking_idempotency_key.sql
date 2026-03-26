ALTER TABLE booking
    ADD COLUMN idempotency_key VARCHAR(128) NULL;

CREATE UNIQUE INDEX uq_booking_passenger_idempotency
    ON booking (passenger_id, idempotency_key);
