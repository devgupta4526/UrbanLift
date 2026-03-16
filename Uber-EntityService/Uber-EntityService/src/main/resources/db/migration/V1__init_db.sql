CREATE TABLE booking
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    created_at     datetime NOT NULL,
    updated_at     datetime NOT NULL,
    booking_status ENUM('SCHEDULED','CANCELLED','CAB_ARRIVED','ASSIGNING_DRIVER','IN_RIDE','COMPLETED') NULL,
    booking_date  datetime NOT NULL,
    start_time     datetime NULL,
    end_time       datetime NULL,
    total_distance BIGINT NULL,
    driver_id      BIGINT NULL,
    passenger_id   BIGINT NULL,
    CONSTRAINT pk_booking PRIMARY KEY (id)
);

CREATE TABLE driver
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    created_at     datetime     NOT NULL,
    updated_at     datetime     NOT NULL,
    first_name VARCHAR(255) NULL,
    last_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone_number VARCHAR(255) NULL,
    password VARCHAR(255) NULL,
    address VARCHAR(255) NULL,
    license_number VARCHAR(255) NOT NULL,
    aadhar_card    VARCHAR(255) NULL,
    CONSTRAINT pk_driver PRIMARY KEY (id)
);

CREATE TABLE passenger
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    created_at   datetime     NOT NULL,
    updated_at   datetime     NOT NULL,
    first_name VARCHAR(255) NULL,
    last_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone_number VARCHAR(255) NULL,
    address VARCHAR(255) NULL,
    password     VARCHAR(255) NOT NULL,
    CONSTRAINT pk_passenger PRIMARY KEY (id)
);

ALTER TABLE driver
    ADD CONSTRAINT uc_driver_license_number UNIQUE (license_number);

ALTER TABLE booking
    ADD CONSTRAINT FK_BOOKING_ON_DRIVER FOREIGN KEY (driver_id) REFERENCES driver (id);

ALTER TABLE booking
    ADD CONSTRAINT FK_BOOKING_ON_PASSENGER FOREIGN KEY (passenger_id) REFERENCES passenger (id);

ALTER TABLE booking
    MODIFY booking_date DATE NOT NULL;

ALTER TABLE driver
    CHANGE aadhar_card aadhar_number VARCHAR(255) NOT NULL;

ALTER TABLE booking
    MODIFY start_time DATETIME NOT NULL;

ALTER TABLE booking
    MODIFY end_time DATETIME NOT NULL;