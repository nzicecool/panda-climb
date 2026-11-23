# Panda Climb

**Panda Climb** is a vibrant 3D arcade platformer for Android built with **Kotlin**, **Jetpack Compose**, **OpenGL ES 2.0**, and **Room**. Inspired by classic Donkey Kong tower-climbing action, it puts you in control of an agile heroic panda scaling 5 themed bamboo-and-steel towers to rescue a Red Panda friend from Mecha-Kong.

> **100% Local Persistence:** High scores, 3-star ratings, unlocked towers, career stats, player nickname, and sound settings are stored locally on your device using Room (SQLite)—no sign-in or cloud connection required.

---

## Key Features

- **Real-Time 3D Graphics (OpenGL ES 2.0)**:
  - Custom Blinn-Phong + Fresnel rim lighting and atmospheric biome fog shaders (`ShaderProgram.kt`).
  - Procedural 3D VBO meshes (spheres, cylinders, cones, cubes, tori) for the articulated Panda hero, Mecha-Kong boss, Red Panda shrine, rolling barrels, flame spirits, slanted girders, ladders, springs, and particle bursts.
  - Dynamic perspective camera that tracks the Panda with smooth parallax angles and impact screen shake.
  - Live animated 3D Panda diorama behind the Home screen.
- **Skillful Platforming & Modern Controls**:
  - Fixed 60 Hz physics simulation featuring **coyote time**, **jump buffering**, **variable jump height**, and **mid-air double jumps**.
  - **Jump-Over & Smash Combos**: Leap cleanly over rolling barrels or smash them with the Bamboo Hammer to build combo multipliers up to **`x5`**.
  - Multi-touch arcade D-pad + Jump controls on screen, plus hardware keyboard support (`Arrow Keys` / `WASD` + `Space` / `Z`).
- **4 Collectible Power-Ups**:
  - **Bamboo Hammer**: Spin-smashes barrels and flame spirits for `+250 × combo` bonus points.
  - **Jade Plume**: Grants a mid-air double jump to cross wide gaps or shortcut tiers.
  - **Lotus Shield**: Surrounds Panda with a cyan ring and absorbs one hazard hit.
  - **Blaze Chili**: Boosts run/climb speed and magnetizes nearby Golden Bamboo Shoots.
- **Procedural Arcade Sound Engine**:
  - Synthesizes jump sweeps, spring boings, power-up chimes, barrel crunches, and victory melodies in real time using Android `AudioTrack` with zero external audio assets.
- **On-Device Progression & Hall of Fame**:
  - Sequential tower unlocking with an optional **Unlock All** practice toggle in Tower Select.
  - 3-star mastery ratings, best clear times, career stats, and top local runs persisted in Room.

---

## The 5 Towers

| # | Tower | Biome | Key Mechanics | 3-Star Target |
|---|-------|-------|---------------|---------------|
| 1 | **Sunlit Scaffolding** | Sunset Bamboo | Slanted girders, broken ladders, wooden barrels | 3,200 pts |
| 2 | **Jade Foundry** | Emerald Twilight | Conveyor beams, spring launch pads, fast blue barrels | 4,000 pts |
| 3 | **Crimson Pagoda** | Neon-Red Night | Moving sky elevators, climbing flame spirits, Blaze Chili | 4,600 pts |
| 4 | **Aurora Sky-Bridge** | Aurora Observatory | Crumbling bamboo bridges, vertical spike barrels | 5,200 pts |
| 5 | **Iron Summit Finale** | Volcanic Gold | All hazards + **3 Golden Rivet Switches** & Mecha-Kong boss | 6,500 pts |

---

## Tech Stack & Project Structure

- **Language & UI**: Kotlin, Jetpack Compose, Material Design 3 (`Fredoka` & `Plus Jakarta Sans` bundled in `res/font/`)
- **3D Rendering**: Android OpenGL ES 2.0 (`GLSurfaceView`, custom GLSL vertex/fragment shaders)
- **Local Persistence**: Room (`room-runtime`, `room-ktx`) with KSP
- **Concurrency**: Kotlin Coroutines & `StateFlow`
- **Minimum / Target SDK**: `minSdk = 24` (Android 7.0) · `targetSdk = 36` · `compileSdk = 36` · Java 11

```text
app/src/main/java/com/example/
├── MainActivity.kt                     # Edge-to-edge host, Room/audio wiring & app routing
├── audio/
│   └── ArcadeSoundEngine.kt            # Procedural PCM AudioTrack sound synthesizer
├── data/
│   ├── AppDatabase.kt                  # Room database singleton (panda_climb_local.db)
│   ├── GameDao.kt                      # Reactive Flow queries & suspend writes
│   ├── GameEntities.kt                 # LevelProgress, HighScore & PlayerStats entities
│   └── GameProgressRepository.kt       # Local progression, stars & stats repository
├── game/
│   ├── engine/
│   │   ├── GameModels.kt               # Biome palettes, entities, power-ups & snapshots
│   │   ├── GameSimulation.kt           # Fixed 60 Hz physics, barrel AI & combo engine
│   │   └── LevelCatalog.kt             # 5 handcrafted tower definitions
│   └── gl/
│       ├── MeshBuilder.kt              # VBO cube, sphere, cylinder, cone & torus meshes
│       ├── Panda3DRenderer.kt          # 3D scene, camera, characters & particles renderer
│       └── ShaderProgram.kt            # GLSL lit & atmospheric sky gradient shaders
└── ui/
    ├── PandaClimbViewModel.kt          # StateFlows, simulation sync & navigation state
    ├── screens/
    │   ├── HomeScreen.kt               # Live 3D diorama, nickname editor & power-up guide
    │   ├── LevelSelectScreen.kt        # 5 tower cards, stars & Unlock All toggle
    │   ├── GameScreen.kt               # 3D viewport, multi-touch controls & HUD overlays
    │   └── LeaderboardScreen.kt        # Career stats & local Hall of Fame
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## Building & Running

### Prerequisites
- Android Studio (Ladybug or newer) / Android SDK Platform 36
- JDK 11+

### Commands

```bash
# Compile debug APK
gradle :app:assembleDebug

# Run game simulation unit tests
gradle :app:testDebugUnitTest --tests "com.example.GameSimulationTest"
```

*(Or open the project in Android Studio and click **Run** on an Android 7.0+ device or emulator with OpenGL ES 2.0 support.)*

---

## Publishing the GitHub Page

This repository includes a ready-to-publish static showcase website in `/docs/`:

1. Push this repository to GitHub (including the `/docs/` directory).
2. Open your GitHub repository **Settings → Pages**.
3. Under **Build and deployment**, set **Source** to **Deploy from a branch**.
4. Select branch **`main`** (or your default branch) and folder **`/docs`**, then click **Save**.
5. GitHub will publish your live landing page at `https://<your-username>.github.io/<your-repo>/`.
