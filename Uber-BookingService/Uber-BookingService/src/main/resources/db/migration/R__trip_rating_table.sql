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
