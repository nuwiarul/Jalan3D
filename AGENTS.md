# Jalan3D — Agent Guide

## Project Overview
Android app for reporting road damage with 3D visualization.
- **MapLibre Native** for basemap, terrain, fill-extrusion
- **Filament** (Google) for 3D model rendering (glTF)
- **Jetpack Compose** for UI
- **Kotlin** for main code
- **Rust + Axum 0.8 + SQLx** for backend API

## Tech Stack

### Android
- **JDK:** 17 (Temurin)
- **Gradle:** via wrapper (AGP 8.7.3)
- **Android SDK:** Windows SDK mounted at `/mnt/c/Users/Vinrul/AppData/Local/Android/Sdk/`
- **MapLibre Native:** `org.maplibre.gl:android-sdk:11.x`
- **Filament:** `com.google.android.filament:filament-android`
- **Build:** `cd android && ./gradlew assembleDebug`

### Backend (Rust)
- **Framework:** Axum 0.8
- **Database:** SQLx with multi-db support (SQLite / PostgreSQL / MariaDB)
- **Other deps:** tokio, serde, tower-http, uuid, chrono, reqwest, dotenvy
- **Build:** `cd backend && cargo build`
- **Test:** `cd backend && cargo test && cargo clippy`

## Environment (WSL)
- ANDROID_HOME: `/mnt/c/Users/Vinrul/AppData/Local/Android/Sdk`
- SDK platform: API 24+ (target 36)
- Build tools: 36.0.0
- NDK: available via Windows SDK (symlinked for WSL compat)
- Rust: stable (via rustup)

## Project Structure
```
Jalan3D/
├── AGENTS.md                ← this file
├── android/                 ← Android app
│   ├── app/src/main/java/com/jalan3d/
│   │   ├── MainActivity.kt
│   │   ├── ui/              ← Compose screens
│   │   ├── map/             ← MapLibre + CustomLayer
│   │   ├── model/           ← 3D models (Filament)
│   │   ├── data/            ← API client, repository
│   │   └── camera/          ← CameraX
│   └── app/build.gradle.kts
├── backend/                 ← Rust backend
│   ├── src/
│   │   ├── main.rs
│   │   ├── routes/          ← API endpoints
│   │   ├── models/          ← Data structures
│   │   ├── db/              ← Database queries
│   │   ├── geocode.rs
│   │   └── config.rs
│   ├── Cargo.toml
│   └── migrations/          ← SQLx migrations
├── docs/
│   ├── prd.md               ← Product Requirements Document
│   └── plan.md              ← Implementation plan per PR
└── .github/workflows/
    ├── build-android.yml    ← CI for APK
    └── build-backend.yml    ← CI for Rust
```

## Workflow
1. Agent writes code (Android or Backend) → builds locally
2. Commits → creates PR
3. User reviews → approves → merges
4. GitHub Actions builds APK + runs Rust tests
5. User tests on device → reports bugs via PR comment
6. Agent iterates

## MapLibre SDK
- FillExtrusionLayer for 3D road damage extrusion
- Style: protomaps or demotiles.maplibre.org
- CustomLayer interface for Filament integration
- Terrain (DEM) support for elevation

## Filament Integration
- CustomLayer → access OpenGL projection matrix
- Filament renderer renders glTF model overlay
- Camera sync: MapLibre camera → Filament camera
- Use Filament's gltfio for model loading

## Backend (Rust + Axum + SQLx)
- Axum 0.8: latest stable, async-first
- SQLx: compile-time checked queries, multi-database
  - `sqlx::SqlitePool` for SQLite (dev)
  - `sqlx::PgPool` for PostgreSQL (prod)
  - `sqlx::MySqlPool` for MariaDB (prod)
- Runtime database switching via `DATABASE_URL` env
- Image upload via `axum::extract::Multipart`
- Reverse geocoding via Nominatim (OpenStreetMap)
- API returns JSON, Android uses Retrofit/OkHttp

## Deployment (Backend)
- Build/run via **tmux** di server (WSL), bukan CI
- Android connect ke backend via **Tailscale**
- Backend URL: `http://100.72.147.67:<port>` (WSL Tailscale IP)
- Di Android dev: ganti BASE_URL pake Tailscale IP
