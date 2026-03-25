-- Seed fare_config for every CarType so /fare/estimate works for all vehicle classes.
-- Skips rows that already exist (e.g. manual seeds from docs).

INSERT INTO fare_config (created_at, updated_at, car_type, base_fare, per_km_rate, per_min_rate, surge_multiplier)
SELECT NOW(), NOW(), 'HATCHBACK', 40.00, 12.00, 1.50, 1.00
WHERE NOT EXISTS (SELECT 1 FROM fare_config WHERE car_type = 'HATCHBACK');

INSERT INTO fare_config (created_at, updated_at, car_type, base_fare, per_km_rate, per_min_rate, surge_multiplier)
SELECT NOW(), NOW(), 'SEDAN', 50.00, 15.00, 2.00, 1.00
WHERE NOT EXISTS (SELECT 1 FROM fare_config WHERE car_type = 'SEDAN');

INSERT INTO fare_config (created_at, updated_at, car_type, base_fare, per_km_rate, per_min_rate, surge_multiplier)
SELECT NOW(), NOW(), 'COMPACT_SUV', 60.00, 18.00, 2.50, 1.00
WHERE NOT EXISTS (SELECT 1 FROM fare_config WHERE car_type = 'COMPACT_SUV');

INSERT INTO fare_config (created_at, updated_at, car_type, base_fare, per_km_rate, per_min_rate, surge_multiplier)
SELECT NOW(), NOW(), 'SUV', 80.00, 20.00, 3.00, 1.00
WHERE NOT EXISTS (SELECT 1 FROM fare_config WHERE car_type = 'SUV');

INSERT INTO fare_config (created_at, updated_at, car_type, base_fare, per_km_rate, per_min_rate, surge_multiplier)
SELECT NOW(), NOW(), 'XL', 100.00, 25.00, 4.00, 1.00
WHERE NOT EXISTS (SELECT 1 FROM fare_config WHERE car_type = 'XL');
