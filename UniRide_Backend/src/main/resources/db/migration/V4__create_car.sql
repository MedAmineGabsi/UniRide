-- Feature ID03 - Create Ride Listing: Car class and its creation constraints (TID082, TID084)

CREATE TABLE car (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- A car always has a driver, and a driver can only have one car.
    driver_id INTEGER NOT NULL REFERENCES driver (id) ON DELETE CASCADE,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    color VARCHAR(30) NOT NULL,
    license_plate VARCHAR(20) NOT NULL, -- uniqueness / format: TID083
    -- Total seats in the car, driver's seat included: at least the driver plus one passenger.
    seat_capacity INTEGER NOT NULL,
    CONSTRAINT car_driver_id_key UNIQUE (driver_id),
    CONSTRAINT car_seat_capacity_check CHECK (seat_capacity > 1),
    -- Required text fields cannot be empty or only spaces either.
    CONSTRAINT car_make_not_blank CHECK (trim(make) <> ''),
    CONSTRAINT car_model_not_blank CHECK (trim(model) <> ''),
    CONSTRAINT car_color_not_blank CHECK (trim(color) <> ''),
    CONSTRAINT car_license_plate_not_blank CHECK (trim(license_plate) <> '')
);
