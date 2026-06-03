-- Create reports table
CREATE TABLE IF NOT EXISTS reports (
    id          TEXT PRIMARY KEY,  -- UUID
    lat         REAL NOT NULL,
    lng         REAL NOT NULL,
    severity    TEXT NOT NULL,     -- 'ringan'|'sedang'|'berat'|'kritis'
    photo_path  TEXT,
    address     TEXT,
    description TEXT,
    status      TEXT DEFAULT 'pending', -- pending|verified|fixed
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL
);
