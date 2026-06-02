# Jalan3D — Agent Guide

## Project Overview
Android app for reporting road damage with 3D visualization.
- **MapLibre Native** for basemap, terrain, fill-extrusion
- **Filament** (Google) for 3D model rendering (glTF)
- **Jetpack Compose** for UI
- **Kotlin** for main code
- Kotlin coroutines + MVVM architecture

## Tech Stack
- **JDK:** 17 (Temurin)
- **Gradle:** via wrapper (AGP 8.7.3)
- **Android SDK:** Windows SDK mounted at `/mnt/c/Users/Vinrul/AppData/Local/Android/Sdk/`
- **MapLibre Native:** Android SDK (maplibre-gl-android)
- **Filament:** Google Filament for Android
- **Build:** `./gradlew assembleDebug`

## Environment (WSL)
- ANDROID_HOME: `/mnt/c/Users/Vinrul/AppData/Local/Android/Sdk`
- SDK platform: API 24+ (target 36)
- Build tools: 36.0.0
- NDK: available via Windows SDK (symlinked for WSL compat)

## Project Structure
```
Jalan3D/
├── AGENTS.md          ← this file
├── app/
│   ├── src/main/java/com/jalan3d/
│   │   ├── MainActivity.kt
│   │   ├── ui/           ← Compose screens
│   │   ├── map/          ← MapLibre + CustomLayer setup
│   │   ├── model/        ← 3D models (Filament)
│   │   ├── data/         ← Repository, API, local DB
│   │   └── camera/       ← CameraX for photo capture
│   ├── src/main/assets/  ← glTF models, map style
│   └── build.gradle.kts
├── docs/
│   ├── prd.md            ← Product Requirements Document
│   └── plan.md           ← Implementation plan per PR
├── build.gradle.kts      ← root
└── settings.gradle.kts
```

## Workflow
1. Agent writes code → builds locally (`./gradlew assembleDebug`)
2. Commits → creates PR
3. User reviews → approves → merges
4. GitHub Actions builds APK (artifact)
5. User tests on device → reports bugs via PR comment
6. Agent iterates

## MapLibre SDK
- Dependency: `org.maplibre.gl:android-sdk:11.x`
- FillExtrusionLayer for 3D road damage extrusion
- Style: protomaps or demotiles.maplibre.org
- CustomLayer interface for Filament integration
- Terrain (DEM) support for elevation

## Filament Integration
- CustomLayer → access OpenGL projection matrix
- Filament renderer renders glTF model overlay
- Camera sync: MapLibre camera → Filament camera
- No native glTF loader in MapLibre; use Filament's gltfio
