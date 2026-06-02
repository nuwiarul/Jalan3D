# Jalan3D — Product Requirements Document

> **Version:** 1.0
> **Status:** Draft
> **Author:** nuwiarul

---

## 1. Vision

Aplikasi Android untuk melaporkan dan memvisualisasikan **kerusakan jalan** dalam bentuk **3D interactive map**. Pengguna bisa melihat lokasi, tingkat keparahan, dan foto kerusakan dalam tampilan 3D yang imersif.

## 2. Target User

- **Pengendara** (motor/mobil) — melapor jalan rusak yang ditemui
- **Warga** — memantau kondisi jalan di sekitar
- **Pemerintah Desa/Kota** — melihat prioritas perbaikan

## 3. Core Features (MVP)

### 3.1 Map View
- Map interaktif pake **MapLibre Native**
- Terrain 3D (elevasi medan)
- Basemap dari OpenStreetMap tiles

### 3.2 Road Damage Visualization (3D)
- **Fill-extrusion** berdasarkan tingkat kerusakan:
  - 🟢 **Ringan** — extrusion 0.5m (retak kecil)
  - 🟡 **Sedang** — extrusion 1.5m (retak lebar)
  - 🟠 **Berat** — extrusion 3m (berlubang)
  - 🔴 **Kritis** — extrusion 5m (ambrol/putus)
- Warna extrusion sesuai severity
- Marker + foto tiap laporan

### 3.3 Report Feature
- Tombol "Lapor" → pilih lokasi di map
- Ambil foto (CameraX)
- Pilih tingkat kerusakan (Ringan/Sedang/Berat/Kritis)
- Kirim laporan
- Lokasi otomatis dari map (bisa digeser manual)

### 3.4 3D Model Close-up (Filament)
- Tap laporan → muncul **3D model lubang** (glTF)
- Model bervariasi sesuai severity
- Orbit camera di model 3D
- Overlay info: alamat, tanggal, status

### 3.5 Data Layer
- Data disimpan di **Firebase Firestore** atau **Supabase**
- Setiap laporan: {id, lat, lng, severity, foto_url, timestamp, status, alamat}
- List view: daftar laporan terbaru

## 4. Future Features (Post-MVP)

- **Offline map** (tile caching)
- **Gamification** (poin pelapor)
- **Route planner** hindari jalan rusak
- **AI deteksi lubang** dari foto
- **Export report** untuk dinas terkait
- **Komunitas** — vote/konfirmasi laporan

## 5. Non-Functional Requirements

| Aspek | Target |
|-------|--------|
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 |
| **Build tools** | 36.0.0 |
| **MapLibre SDK** | 11.x |
| **Filament** | 1.54.x |
| **Performance** | 60fps map + 3D overlay |
| **APK size** | < 30MB (debug) |
| **Storage** | < 50MB cache |

## 6. Architecture

```
User → Compose UI → ViewModel → Repository → 
  ├── MapLibre (map rendering)
  ├── Filament (3D model overlay)
  ├── CameraX (photo capture)
  └── Backend (Firebase/Supabase)

MapLibre ← CustomLayer → OpenGL ES → Filament Renderer
```

## 7. Milestones

| Fase | Fitur | Target PR |
|------|-------|-----------|
| P0 | Scaffold + MapLibre basemap | PR 1 |
| P0 | Map markers + report form | PR 2 |
| P0 | 3D fill-extrusion jalan rusak | PR 3 |
| P1 | Filament 3D model close-up | PR 4 |
| P1 | Backend integration (CRUD) | PR 5 |
| P2 | List view + history | PR 6 |
| P2 | Polish + testing | PR 7 |
