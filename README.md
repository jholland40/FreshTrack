# FreshTrack Android App

FreshTrack is a Kotlin & Jetpack Compose Android application for pantry inventory, expiration date tracking, and food waste reduction.

---

## 👥 Team Responsibilities & Code Base Ownership

This project uses a **Package-by-Feature** architecture.

| Team Member | Area of Responsibility | Target Package Path |
| :--- | :--- | :--- |
| **Member 1** | Database schema, UPC codes database, Room Entities, DAOs, Repository layer & Migrations | `app/src/main/java/com/example/freshtrack/core/database/`<br>`app/src/main/java/com/example/freshtrack/core/data/` |
| **Member 2** | Expiration notifications, WorkManager, deep links & User preferences (DataStore) | `app/src/main/java/com/example/freshtrack/core/notifications/`<br>`app/src/main/java/com/example/freshtrack/core/datastore/`<br>`app/src/main/java/com/example/freshtrack/feature/settings/` |
| **Member 3** | Barcode recognition, CameraX preview, ML Kit OCR & Confirmation workflow | `app/src/main/java/com/example/freshtrack/feature/scanner/` |
| **Member 4** | UI design system, Pantry wireframes, Navigation flows, Screen states & Accessibility | `app/src/main/java/com/example/freshtrack/feature/pantry/`<br>`app/src/main/java/com/example/freshtrack/feature/insights/`<br>`app/src/main/java/com/example/freshtrack/navigation/` |
| **Lead Developer** | Application orchestration, Dependency Injection, Shell UI & Activity wiring | `app/src/main/java/com/example/freshtrack/MainActivity.kt`<br>`app/src/main/java/com/example/freshtrack/ui/` |

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/freshtrack.git
   ```
2. Open the project in **Android Studio Ladybug (2024.2.1+)** or higher.
3. Sync Gradle and run on an Android device or emulator running **API 26 (Android 8.0)** or higher.

---

## 🌿 Git Branching Workflow

- `main`: Production-ready stable codebase.
- `feature/<domain-name>`: Create feature branches corresponding to your assigned package directory (e.g. `feature/camerax-scanner`, `feature/room-database-migration`).
- Open a Pull Request into `main` when your feature is complete and verified.
