-- TID027: Ride entity - estimated trip duration, in minutes.
ALTER TABLE ride
    ADD COLUMN duration_minutes INTEGER;

UPDATE ride
SET duration_minutes = 60
WHERE duration_minutes IS NULL;

ALTER TABLE ride
    ALTER COLUMN duration_minutes SET NOT NULL;

ALTER TABLE ride
    ADD CONSTRAINT ride_duration_minutes_check CHECK (duration_minutes > 0);
