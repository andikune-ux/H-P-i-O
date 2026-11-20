# H P i O — Hybrid OS + Liquid Glass Android Launcher

**H P i O** adalah aplikasi **Native Android Home Launcher** milik **Andik Une** yang memadukan fleksibilitas Android Launcher modern dengan estetika **Hybrid OS + Liquid Glass**.

- **Package / Application ID**: `dev.andikune.launcherhpio`
- **Repository**: `andikune-ux/H-P-i-O`
- **Engineering Standard**: [AI Rulebook V3](https://github.com/andikune-ux/ai-rulebook) (Commit SHA: `4133a27721a40ab356ac46f0525c450cb63c5f7c`)

## Fitur Utama

- **Home Screen Multi-Halaman**: `HorizontalPager`, Hero Clock & Status, Widget bawaan Hybrid OS (Telemetry RAM/Storage/Baterai, Catatan Kaca Cepat), dukungan `AppWidgetHost` sistem, Folder kaca interaktif, Dock mengambang, grid & ukuran ikon yang dapat disesuaikan.
- **App Drawer Hybrid**: Pencarian instan, filter kategori (`Komunikasi`, `Media`, `Produktivitas`, `Utilitas`, `Gim`, `Sistem`), pengurutan (`A–Z`, `Z–A`, `Baru Diinstal`, `Baru Diperbarui`), serta dua pemicu yang dapat dikonfigurasi (**Swipe Up** dan **Tombol App Drawer**).
- **Control Center Liquid Glass**: Status Wi-Fi & panel konektivitas, Bluetooth, Senter (`CameraManager`), slider kecerahan, slider volume media & dering (`AudioManager`), serta pintasan pengaturan Android resmi.
- **Personalisasi**: Mode Sistem / Terang / Gelap, 3 wallpaper Liquid Glass bawaan + Android Photo Picker (tanpa izin storage luas), kontrol transparansi & blur, animasi laci aplikasi, dan gestur.
- **Lima Kapabilitas Wajib AI Rulebook V3**:
  1. Manajemen Izin & Kapabilitas Minimum (`PermissionCapabilityHelper`).
  2. Pemisahan Helper & Repository Modular (`AppStorageHelper`, `CrashRecoveryHelper`, `GitHubReleaseUpdateHelper`, `InstalledAppsRepository`, dll.).
  3. Crash Recovery & Viewer lokal dengan penyensoran data sensitif, tombol **Salin Log**, **Kembali**, dan proteksi crash loop.
  4. Notifikasi Update asinkron dari GitHub Releases resmi `andikune-ux/H-P-i-O` dengan tombol **Skip**, **Nanti**, dan **Update**.
  5. Penyimpanan Privat Terstruktur di `filesDir/H P i O/` (`Pembaruan Aplikasi/` & `Log Crash/`).

## Build Lokal

```bash
gradle testDebugUnitTest lintDebug assembleDebug
```

Untuk dokumentasi build, signing Andik Une, dan rilis GitHub Actions, lihat `docs/BUILD_GUIDE.md` dan `docs/RELEASE_GUIDE.md`.
