-- Seats now come from the driver's car (car.seat_capacity, V4), so driver.car_seats (V3) is no longer used.
ALTER TABLE driver
    DROP COLUMN IF EXISTS car_seats;
