ALTER TABLE persistedglobalgroup ADD COLUMN  IF NOT EXISTS isRegEx BOOLEAN NOT NULL DEFAULT FALSE;