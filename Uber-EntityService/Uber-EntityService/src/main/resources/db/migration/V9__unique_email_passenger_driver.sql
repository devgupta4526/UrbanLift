-- Enforce one account per email (application also checks before insert).
-- If this fails, remove duplicate rows manually then re-run.

CREATE UNIQUE INDEX uc_passenger_email ON passenger (email);
CREATE UNIQUE INDEX uc_driver_email ON driver (email);
