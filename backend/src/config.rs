#[derive(Debug, Clone, PartialEq)]
pub enum DatabaseKind {
    Sqlite,
    Postgres,
    Mysql,
}

impl DatabaseKind {
    pub fn detect(url: &str) -> Self {
        if url.starts_with("postgres://") || url.starts_with("postgresql://") {
            Self::Postgres
        } else if url.starts_with("mysql://") {
            Self::Mysql
        } else {
            // Default: sqlite:// or anything else
            Self::Sqlite
        }
    }

    pub fn display_name(&self) -> &str {
        match self {
            Self::Sqlite => "SQLite (development)",
            Self::Postgres => "PostgreSQL (production)",
            Self::Mysql => "MariaDB/MySQL (production)",
        }
    }

    pub fn is_production(&self) -> bool {
        matches!(self, Self::Postgres | Self::Mysql)
    }
}

#[derive(Debug, Clone)]
pub struct Config {
    pub database_url: String,
    pub database_kind: DatabaseKind,
    pub host: String,
    pub port: u16,
    pub upload_dir: String,
}

impl Config {
    pub fn from_env() -> Self {
        let database_url = std::env::var("DATABASE_URL")
            .unwrap_or_else(|_| "sqlite:///data/jalan3d.db".to_string());
        let database_kind = DatabaseKind::detect(&database_url);

        Self {
            database_url,
            database_kind,
            host: std::env::var("HOST").unwrap_or_else(|_| "0.0.0.0".to_string()),
            port: std::env::var("PORT")
                .ok()
                .and_then(|p| p.parse().ok())
                .unwrap_or(3000),
            upload_dir: std::env::var("UPLOAD_DIR")
                .unwrap_or_else(|_| "./uploads".to_string()),
        }
    }
}
