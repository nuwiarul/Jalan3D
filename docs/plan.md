# Jalan3D — Implementation Plan

> **Phase:** Planning
> **Author:** nuwiarul
> **Tech Stack:** Kotlin, Jetpack Compose, MapLibre Native, Filament, Firebase/Supabase

---

## PR 1: Project Scaffold + MapLibre Basemap

**Goal:** Project Android functional dengan MapLibre map di Compose

### Task 1.1 — Create project skeleton
- Root build.gradle.kts (AGP 8.7.3, Kotlin 2.1.0)
- App build.gradle.kts (dependencies: MapLibre, Compose, Coroutines)
- AndroidManifest.xml (permission internet, camera, location)
- MainActivity.kt (Compose entry point)
- settings.gradle.kts
- gradle.properties (AndroidX, Compose, suppress compileSdk warning)
- local.properties (sdk.dir)

### Task 1.2 — MapLibre Compose wrapper
- `MapScreen.kt` — Composable dengan MapLibre map view
- `MapViewModel.kt` — state management
- Dummy style: `https://demotiles.maplibre.org/style.json`
- Camera default ke lokasi Indonesia (Jakarta / Bali)

### Task 1.3 — App navigation scaffold
- Bottom bar: Map, List, Profile (placeholder)
- Simple navigation via NavHost

### Task 1.4 — GitHub Actions CI
- Workflow `.github/workflows/build.yml`
- Trigger: push ke PR
- Build APK and upload artifact

**Files:**
- `build.gradle.kts` (root)
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/jalan3d/MainActivity.kt`
- `app/src/main/java/com/jalan3d/ui/MapScreen.kt`
- `app/src/main/java/com/jalan3d/ui/MapViewModel.kt`
- `app/src/main/java/com/jalan3d/ui/HomeScreen.kt` (navigation host)
- `settings.gradle.kts`
- `gradle.properties`
- `local.properties`
- `.github/workflows/build.yml`

**Verification:** `./gradlew assembleDebug` ✅

---

## PR 2: Map Markers + Report Feature

**Goal:** Pengguna bisa lapor jalan rusak via map

### Task 2.1 — Location picker
- Tap location on map → get lat/lng
- Pin marker at tapped location
- Reverse geocode (Nominatim API) for address

### Task 2.2 — Report form
- Bottom sheet with: severity selector, camera button, submit
- CameraX integration to capture photo
- Save to device cache
- Form validation

### Task 2.3 — Map markers from reports
- SymbolLayer for existing reports on map
- Different icons per severity level
- Tap marker → show info popup

**Files:**
- `app/src/main/java/com/jalan3d/data/Report.kt` — data class
- `app/src/main/java/com/jalan3d/data/Severity.kt` — enum
- `app/src/main/java/com/jalan3d/ui/ReportFormSheet.kt` — bottom sheet
- `app/src/main/java/com/jalan3d/camera/PhotoCapture.kt`
- `app/src/main/java/com/jalan3d/map/MapMarkers.kt`
- `app/src/main/java/com/jalan3d/map/MapClickListener.kt`

**Verification:** APK test — lapor jalan, lihat marker di map

---

## PR 3: 3D Fill-Extrusion Road Damage

**Goal:** Visualisasi 3D jalan rusak pake fill-extrusion

### Task 3.1 — Data format untuk extrusion
- Convert report lat/lng → polygon/bbox
- Severity → extrusion height mapping
- Color mapping per severity

### Task 3.2 — FillExtrusionLayer
- Add FillExtrusionLayer to map style
- Polygon source dari laporan
- Height = severity * multiplier
- Color gradient (hijau→merah)

### Task 3.3 — 3D camera interaction
- Double-tap laporan → fly to 3D angle (pitch 60°)
- Smooth camera animation
- Terrain DEM untuk elevasi akurat

**Files:**
- `app/src/main/java/com/jalan3d/map/ExtrusionLayer.kt`
- `app/src/main/java/com/jalan3d/map/Map3DController.kt`
- `app/src/main/java/com/jalan3d/data/ExtrusionData.kt`

**Verification:** APK test — laporan kelihatan extrusion 3D di map

---

## PR 4: Filament 3D Model Close-up

**Goal:** Tap laporan → lihat model 3D lubang (glTF)

### Task 4.1 — Filament setup
- Add Filament dependency
- CustomLayer untuk akses OpenGL
- Filament renderer lifecycle

### Task 4.2 — glTF model loading
- Asset glTF files (template: lubang kecil/besar)
- Use Filament gltfio to load
- Position model at report location
- Scale based on severity

### Task 4.3 — Camera sync
- MapLibre camera → Filament camera
- Tap report → animate to position
- Orbit controls for 3D model

### Task 4.4 — Info overlay
- Card with photo + severity + address
- Status badge (pending/selesai)

**Files:**
- `app/src/main/java/com/jalan3d/model/ModelRenderer.kt`
- `app/src/main/java/com/jalan3d/model/ModelLoader.kt`
- `app/src/main/java/com/jalan3d/model/FilamentRenderer.kt`
- `app/src/main/java/com/jalan3d/map/FilamentCustomLayer.kt`
- `app/src/main/assets/models/lubang_ringan.gltf`
- `app/src/main/assets/models/lubang_berat.gltf`

**Verification:** APK test — tap marker, muncul 3D model

---

## PR 5: Backend Integration

**Goal:** Data laporan tersimpan di cloud

### Task 5.1 — Firebase/Supabase setup
- Project config
- Data schema: `reports/{id}`
- CRUD operations

### Task 5.2 — Repository layer
- `ReportRepository.kt`
- Upload foto (Firebase Storage / Supabase Storage)
- Sync: submit report → upload foto → save to DB

### Task 5.3 — Data binding
- Map screen reads from repository
- New report writes to repository
- Loading / error states

**Files:**
- `app/src/main/java/com/jalan3d/data/ReportRepository.kt`
- `app/src/main/java/com/jalan3d/data/ReportApi.kt`
- `app/src/main/java/com/jalan3d/data/FirebaseModule.kt` (or Supabase)

**Verification:** Submit report → lihat di Firebase console

---

## PR 6: List View + History

**Goal:** Daftar laporan dalam bentuk list

- LazyColumn with reports
- Filter by severity / date
- Tap → show detail + map location
- Empty state, loading state

---

## PR 7: Polish + Testing

**Goal:** Aplikasi stabil dan siap demo

- Loading/error/success states all screens
- Dark mode support
- Memory management (bitmap, glTF cache)
- ProGuard rules
- Edge cases: no internet, no GPS, no camera
- Final full test build

## Key Decisions

| Aspek | Keputusan | Alasan |
|-------|-----------|--------|
| Map style | OpenStreetMap tiles (protomaps) | Gratis, ringan, tanpa API key |
| Backend MVP | Firebase (dulu) | Cepat setup, integrasi mudah |
| 3D model | glTF template + Filament | MapLibre gak support native 3D |
| Location | Reverse geocode + manual pick | Gak perlu GPS akurat |
| Extrusion data | Polygon dari bbox lat/lng | Simpel, gak perlu road graph |
