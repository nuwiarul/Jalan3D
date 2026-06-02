# Jalan3D — Implementation Plan

> **Phase:** Planning
> **Author:** nuwiarul
> **Tech Stack:** Kotlin + Jetpack Compose + MapLibre Native + Filament + **Rust + Axum 0.8 + SQLx**

---

## Repository Structure

```
Jalan3D/
├── AGENTS.md
├── android/                    ← Android app
│   ├── app/src/main/java/com/jalan3d/
│   └── build.gradle.kts
├── backend/                    ← Rust backend
│   ├── src/
│   ├── Cargo.toml
│   └── migrations/
├── docs/
│   ├── prd.md
│   └── plan.md
└── .github/workflows/
    ├── build-android.yml       ← Build APK
    └── build-backend.yml       ← Cargo test + clippy
```

---

## PR 1: Android Scaffold + MapLibre Basemap

**Goal:** Project Android functional dengan MapLibre map di Compose

### Task 1.1 — Create project skeleton
- Root build.gradle.kts (AGP 8.7.3, Kotlin 2.1.0)
- App build.gradle.kts (dependencies: MapLibre, Compose, Coroutines, Retrofit)
- AndroidManifest.xml (permission internet, camera, location)
- MainActivity.kt (Compose entry point)
- settings.gradle.kts + gradle.properties + local.properties

### Task 1.2 — MapLibre Compose wrapper
- `MapScreen.kt` — Composable dengan MapLibre map view
- `MapViewModel.kt` — state management
- Style: `https://demotiles.maplibre.org/style.json`
- Camera default ke Indonesia (Jakarta / Bali)

### Task 1.3 — App navigation scaffold
- Bottom bar: Map, List, Profile (placeholder)
- NavHost navigation

### Task 1.4 — GitHub Actions CI (Android)
- Workflow `.github/workflows/build-android.yml`
- Trigger: PR ke main
- Build APK + upload artifact

**Files:**
- `android/build.gradle.kts` (root)
- `android/app/build.gradle.kts`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/com/jalan3d/MainActivity.kt`
- `android/app/src/main/java/com/jalan3d/ui/MapScreen.kt`
- `android/app/src/main/java/com/jalan3d/ui/MapViewModel.kt`
- `android/app/src/main/java/com/jalan3d/ui/HomeScreen.kt`
- `android/settings.gradle.kts`
- `android/gradle.properties`
- `android/local.properties`
- `.github/workflows/build-android.yml`

**Verification:** `cd android && ./gradlew assembleDebug` ✅

---

## PR 2: Backend (Rust + Axum 0.8 + SQLx)

**Goal:** REST API backend dengan multi-database support

### Task 2.1 — Project scaffold
- `Cargo.toml` dengan dependencies:
  - `axum = "0.8"` (latest)
  - `sqlx` dengan feature: `sqlite`, `postgres`, `mysql`, `runtime-tokio`, `migrate`
  - `tokio`, `serde`, `serde_json`, `tower-http` (CORS)
  - `uuid`, `chrono`, `image` (resize foto)
  - `tracing`, `tracing-subscriber`
  - `reqwest` (Nominatim API)
  - `dotenvy` (env config)
  - `multipart` via `axum-extra` atau `tower-http`
- rust-toolchain.toml (stable)
- `.env.example` dengan konfigurasi database

### Task 2.2 — Database layer (SQLx)
- Migration files: `migrations/001_create_reports.sql`
- Schema:
  ```sql
  CREATE TABLE reports (
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
  ```
- SQLx offline mode (`sqlx prepare`) untuk compile-time check
- Query builder pattern: satu file query per database type (opsional)

### Task 2.3 — Multi-database config
- Database URL dari env `DATABASE_URL`
- Auto-detect: sqlite:// → SQLite, postgres:// → PostgreSQL, mysql:// → MariaDB
- Runtime switching via enum `DatabaseKind`
- SQLite untuk dev lokal, PostgreSQL/MariaDB untuk production

### Task 2.4 — API Routes (Axum 0.8)
```
GET    /api/health                 → Health check
GET    /api/reports                → List reports (filter? severity, status, nearby?lat,lng,radius)
GET    /api/reports/:id            → Detail report
POST   /api/reports                → Create report (JSON body, no photo)
PUT    /api/reports/:id            → Update report status
DELETE /api/reports/:id            → Delete report (admin)
POST   /api/upload                 → Upload photo (multipart), return URL
```

### Task 2.5 — Image upload
- Multipart upload via `axum::extract::Multipart`
- Simpan di `uploads/` directory
- Resize ke max 1920px (compress)
- Generate UUID filename

### Task 2.6 — Reverse Geocoding
- Integrasi Nominatim API: `https://nominatim.openstreetmap.org/reverse`
- Endpoint yang memanggil Nominatim berdasarkan lat/lng
- Cache sederhana di memory (HashMap dengan TTL)

### Task 2.7 — CI (Rust)
- `.github/workflows/build-backend.yml`
- Trigger: PR ke main
- Steps: cargo check → cargo clippy → cargo test
- Cache cargo registry

**Files:**
- `backend/Cargo.toml`
- `backend/rust-toolchain.toml`
- `backend/src/main.rs`
- `backend/src/routes/mod.rs`
- `backend/src/routes/reports.rs`
- `backend/src/routes/health.rs`
- `backend/src/routes/upload.rs`
- `backend/src/models/mod.rs`
- `backend/src/models/report.rs`
- `backend/src/db/mod.rs`
- `backend/src/db/pool.rs`
- `backend/src/db/reports.rs`
- `backend/src/geocode.rs`
- `backend/src/config.rs`
- `backend/src/error.rs`
- `backend/migrations/001_create_reports.sql`
- `backend/.env.example`
- `backend/.gitignore`
- `.github/workflows/build-backend.yml`

**Verification:** `cd backend && cargo test && cargo clippy` ✅

---

## PR 3: Map Markers + Report Form (Android) + API Binding

**Goal:** Lapor jalan rusak via map, data tersimpan ke backend Rust

### Task 3.1 — Retrofit API client
- `ReportApi.kt` — Retrofit interface
- `ReportDto.kt` — data transfer objects
- `OkHttpClient` dengan logging interceptor

### Task 3.2 — Location picker
- Tap location on map → get lat/lng
- Pin marker at tapped location
- Reverse geocode lewat backend API

### Task 3.3 — Report form
- Bottom sheet: severity selector + camera button + submit
- CameraX integration to capture photo
- Upload photo via multipart
- Submit report via API

### Task 3.4 — Map markers from API
- SymbolLayer for existing reports
- Icons per severity level
- Tap marker → info popup

### Task 3.5 — Repository layer
- `ReportRepository.kt` (Android)
- Fetch from backend API
- Cache locally (Room atau in-memory)

**Files:**
- `android/app/src/main/java/com/jalan3d/data/api/ReportApi.kt`
- `android/app/src/main/java/com/jalan3d/data/api/ReportDto.kt`
- `android/app/src/main/java/com/jalan3d/data/Report.kt`
- `android/app/src/main/java/com/jalan3d/data/Severity.kt`
- `android/app/src/main/java/com/jalan3d/data/ReportRepository.kt`
- `android/app/src/main/java/com/jalan3d/ui/ReportFormSheet.kt`
- `android/app/src/main/java/com/jalan3d/camera/PhotoCapture.kt`
- `android/app/src/main/java/com/jalan3d/map/MapMarkers.kt`
- `android/app/src/main/java/com/jalan3d/map/MapClickListener.kt`

**Verification:** APK test — lapor jalan → data masuk ke backend SQLite ✅

---

## PR 4: 3D Fill-Extrusion Road Damage (Android)

**Goal:** Visualisasi 3D jalan rusak di map pake fill-extrusion

### Task 4.1 — Extrusion data pipeline
- Fetch reports from API → generate polygons (bbox around lat/lng)
- Severity → extrusion height mapping
- Color mapping per severity

### Task 4.2 — FillExtrusionLayer
- Add FillExtrusionLayer ke map style
- Polygon source dari data laporan
- Height = severity * multiplier: ringan=0.5, sedang=1.5, berat=3, kritis=5 (meter)
- Color gradient (hijau→kuning→merah→ungu)

### Task 4.3 — 3D camera interaction
- Tap laporan → fly to 3D angle (pitch 60°)
- Smooth camera animation
- Optional: Terrain DEM untuk elevasi akurat

**Files:**
- `android/app/src/main/java/com/jalan3d/map/ExtrusionLayer.kt`
- `android/app/src/main/java/com/jalan3d/map/Map3DController.kt`
- `android/app/src/main/java/com/jalan3d/data/ExtrusionData.kt`

**Verification:** APK test — laporan tampil sebagai extrusion 3D ✅

---

## PR 5: Filament 3D Model Close-up (Android)

**Goal:** Tap laporan → lihat model 3D lubang (glTF) pake Filament

### Task 5.1 — Filament setup
- Add Filament dependency (`com.google.android.filament:filament-android`)
- OpenGL context management
- CustomLayer untuk akses OpenGL dari MapLibre

### Task 5.2 — glTF model loading
- Template glTF assets: lubang ringan, sedang, berat
- Use Filament `gltfio` (AssetLoader) to load models
- Position and scale model berdasarkan report location + severity

### Task 5.3 — Camera sync
- MapLibre camera projection → Filament camera
- Orbit controls for 3D model inspection

### Task 5.4 — Info overlay
- Card with photo + severity + address
- Status badge (pending / verified / fixed)

**Files:**
- `android/app/src/main/java/com/jalan3d/model/FilamentRenderer.kt`
- `android/app/src/main/java/com/jalan3d/model/ModelLoader.kt`
- `android/app/src/main/java/com/jalan3d/model/gltf/AssetLoader.kt`
- `android/app/src/main/java/com/jalan3d/map/FilamentCustomLayer.kt`
- `android/app/src/main/assets/models/lubang_ringan.gltf`
- `android/app/src/main/assets/models/lubang_berat.gltf`

**Verification:** APK test — tap marker jalan rusak, muncul 3D model ✅

---

## PR 6: List View + History + Polish

**Goal:** Daftar laporan dan final polish

### Task 6.1 — Report list screen
- LazyColumn dengan reports dari API
- Filter by severity / date / status
- Pull-to-refresh
- Empty state + loading state + error state

### Task 6.2 — Report detail screen
- Full detail: foto, map, severity, address, description, timeline
- Edit status (admin)

### Task 6.3 — Polish
- Dark mode support
- Loading/shimmer animations
- ProGuard rules
- Edge cases: no internet, no GPS, no camera
- Performance optimization (bitmap caching, lazy loading)

**Files:**
- `android/app/src/main/java/com/jalan3d/ui/ReportListScreen.kt`
- `android/app/src/main/java/com/jalan3d/ui/ReportDetailScreen.kt`
- `android/app/src/main/java/com/jalan3d/ui/Navigation.kt`

**Verification:** APK test — full flow: buka app → lihat map → lapor → lihat di list ✅

---

## Key Decisions

| Aspek | Keputusan | Alasan |
|-------|-----------|--------|
| Android Map | MapLibre Native | Open source, 3D support, bebas API key |
| 3D Model | glTF + Filament | GLTF format, Filament PBR engine |
| Backend framework | Axum 0.8 | Async, latest, ergonomic |
| Database | SQLx (SQLite/PostgreSQL/MariaDB) | SQLite dev gampang, SQLx compile-time check |
| Image storage | Disk (dev) / S3 (prod) | Simpel dulu, scalable nanti |
| Reverse geocode | Nominatim | Gratis, OSM-based |
| Extrusion data | Bbox polygon dari lat/lng | Simpel, gak perlu road network data |
| Repo structure | Monorepo (android/ + backend/) | Satu repo, unified PR workflow |
