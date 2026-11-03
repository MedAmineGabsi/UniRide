-- Feature ID03 - Create Ride Listing: license_plate constraints (TID083)
-- A license plate is required and unique.

-- Required: besides NOT NULL (V4), it must contain something other than spaces and hyphens.
ALTER TABLE car
    ADD CONSTRAINT car_license_plate_required CHECK (
        regexp_replace(license_plate, '[[:space:]-]', '', 'g') <> ''
    );

-- Unique, ignoring case, spaces and hyphens: "abc 123", "ABC123" and "ABC-123" are the same plate.
CREATE UNIQUE INDEX car_license_plate_key ON car (
    upper(regexp_replace(license_plate, '[[:space:]-]', '', 'g'))
);
