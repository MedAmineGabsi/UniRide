-- Feature ID03 - Create Ride Listing: ride constraints and driver car seats

-- Number of passenger seats in the driver's car (driver's own seat excluded).
-- Nullable so existing drivers stay valid; when set, a ride cannot offer more seats than this.
ALTER TABLE driver
    ADD COLUMN car_seats INTEGER CHECK (car_seats > 0);

-- Price per seat must be strictly positive (V1 allowed 0).
ALTER TABLE ride
    DROP CONSTRAINT IF EXISTS ride_price_per_seat_check;

ALTER TABLE ride
    ADD CONSTRAINT ride_price_per_seat_check CHECK (price_per_seat > 0);

-- Origin and destination must differ.
ALTER TABLE ride
    ADD CONSTRAINT ride_origin_destination_check CHECK (
        lower(trim(origin)) <> lower(trim(destination))
    );
