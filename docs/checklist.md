# Jalan3D — Progress Checklist

## PR 1: Android Scaffold + MapLibre Basemap
- [x] **1.1** — Create project skeleton ✅
- [x] **1.2** — MapLibre Compose wrapper (MapScreen.kt, MapViewModel.kt, camera 3D view) ✅
- [x] **1.3** — App navigation scaffold (Bottom bar: Map, List, Profile) ✅
- [x] **1.4** — GitHub Actions CI (Android) ✅

## PR 2: Backend (Rust + Axum 0.8 + SQLx)
- [x] **2.1** — Project scaffold (Cargo.toml, rust-toolchain.toml) ✅
- [x] **2.2** — Database layer (SQLx CRUD queries + request DTOs + offline mode) ✅
- [x] **2.3** — Multi-database config (auto-detect SQLite / PostgreSQL / MariaDB) ✅
- [x] **2.4** — API Routes (health, reports CRUD, upload multipart) ✅
- [x] **2.5** — Image upload (multipart, resize 1920px, JPEG quality 85) ✅
- [x] **2.6** — Reverse Geocoding (Nominatim + in-memory cache 24h) ✅

**Catatan:** Build/run backend via tmux di server, bukan CI. Android connect via Tailscale.

## PR 3: Map Markers + Report Form + API Binding
- [x] **3.1** — Retrofit API client (ReportApi, ReportDto, ApiClient, ApiConfig) ✅
- [x] **3.2** — Location picker (tap map → lat/lng + marker + reverse geocode) ✅
- [x] **3.3** — Report form (bottom sheet + severity + camera + submit) ✅
- [x] **3.4** — Map markers from API ✅
- [x] **3.5** — Repository layer ✅

## PR 4: 3D Fill-Extrusion Road Damage
- [x] **4.1** — Extrusion data pipeline ✅
- [x] **4.2** — FillExtrusionLayer ✅
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

**Progress:** 6 / 22 tasks
