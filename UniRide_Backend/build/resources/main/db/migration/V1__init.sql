CREATE TABLE person (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20)
);

CREATE TABLE person_role (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_type VARCHAR(20) NOT NULL CHECK (
        role_type IN ('DRIVER', 'PASSENGER')
    )
);

CREATE TABLE driver (
    id INTEGER PRIMARY KEY REFERENCES person_role (id) ON DELETE CASCADE,
    license_number VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE passenger (
    id INTEGER PRIMARY KEY REFERENCES person_role (id) ON DELETE CASCADE
);

CREATE TABLE person_role_assignment (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_id INTEGER NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES person_role (id) ON DELETE CASCADE,
    UNIQUE (person_id, role_id)
);

CREATE INDEX idx_assignment_role ON person_role_assignment (role_id);

CREATE TABLE ride (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    driver_id INTEGER NOT NULL REFERENCES driver (id),
    origin VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    departure_date_time TIMESTAMP NOT NULL, -- LocalDateTime côté Java
    total_seats INTEGER NOT NULL CHECK (total_seats > 0),
    price_per_seat NUMERIC(10, 2) NOT NULL CHECK (price_per_seat >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (
        status IN (
            'OPEN',
            'FULL',
            'CANCELLED',
            'COMPLETED'
        )
    )
);

CREATE INDEX idx_ride_driver ON ride (driver_id);

CREATE INDEX idx_ride_departure ON ride (departure_date_time);

CREATE TABLE seat_request (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    passenger_id INTEGER NOT NULL REFERENCES passenger (id),
    ride_id INTEGER NOT NULL REFERENCES ride (id),
    seats_requested INTEGER NOT NULL CHECK (seats_requested > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (
        status IN (
            'PENDING',
            'ACCEPTED',
            'REJECTED',
            'CANCELLED'
        )
    )
);

CREATE INDEX idx_seat_request_ride ON seat_request (ride_id);

CREATE INDEX idx_seat_request_passenger ON seat_request (passenger_id);