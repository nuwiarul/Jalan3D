# Jalan3D — Progress Checklist

## PR 1: Android Scaffold + MapLibre Basemap
- [x] **1.1** — Create project skeleton ✅
- [x] **1.2** — MapLibre Compose wrapper (MapScreen.kt, MapViewModel.kt) ✅
- [ ] **1.3** — App navigation scaffold (Bottom bar: Map, List, Profile)
- [x] **1.4** — GitHub Actions CI (Android) ✅

## PR 2: Backend (Rust + Axum 0.8 + SQLx)
- [ ] **2.1** — Project scaffold (Cargo.toml, rust-toolchain.toml)
- [ ] **2.2** — Database layer (SQLx migration + schema)
- [ ] **2.3** — Multi-database config (SQLite / PostgreSQL / MariaDB)
- [ ] **2.4** — API Routes (health, reports CRUD, upload)
- [ ] **2.5** — Image upload (multipart, resize)
- [ ] **2.6** — Reverse Geocoding (Nominatim)
- [ ] **2.7** — CI (Rust)

## PR 3: Map Markers + Report Form + API Binding
- [ ] **3.1** — Retrofit API client
- [ ] **3.2** — Location picker (tap map → lat/lng)
- [ ] **3.3** — Report form (bottom sheet + camera)
- [ ] **3.4** — Map markers from API
- [ ] **3.5** — Repository layer

## PR 4: 3D Fill-Extrusion Road Damage
- [ ] **4.1** — Extrusion data pipeline
- [ ] **4.2** — FillExtrusionLayer
- [ ] **4.3** — 3D camera interaction

## PR 5: Filament 3D Model Close-up
- [ ] **5.1** — Filament setup
- [ ] **5.2** — glTF model loading
- [ ] **5.3** — Camera sync
- [ ] **5.4** — Info overlay

## PR 6: List View + History + Polish
- [ ] **6.1** — Report list screen
- [ ] **6.2** — Report detail screen
- [ ] **6.3** — Polish (dark mode, shimmer, edge cases)

---

**Progress:** 2 / 22 tasks
