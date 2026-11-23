# Changelog — H P i O

## [1.1.10] - 2026-11-23
### Fixed
- Perbaiki identitas versi rilis agar tidak memakai ulang tag v1.1.9 yang sudah terbit.
- Sertakan koreksi scope coroutine pada animasi lensa dock.

## [1.1.9] - 2026-11-23
### Changed
- Pisahkan slider blur wallpaper dari pengaturan material Liquid Glass; tingkat blur kaca tidak lagi dapat diubah pengguna.
- Haluskan gerak lensa dock dengan spring yang lebih terkontrol, magnifikasi ikon kontinu, dan transisi masuk/keluar yang memudar.
- Sesuaikan pantulan warna dock dengan palet material tema agar tidak lagi terpaku pada gradasi biru statis.

## [1.0.0] - 2026-11-20
### Added
- Inisialisasi aplikasi Android Native Home Launcher **H P i O** dengan package `dev.andikune.launcherhpio`.
- Sistem desain **Hybrid OS + Liquid Glass** dengan tipografi lokal `Space Grotesk`, `Plus Jakarta Sans`, dan `JetBrains Mono`, serta adaptive app icon kustom dan 3 wallpaper Liquid Glass bawaan.
- Home Screen multi-halaman (`HorizontalPager`), Hero Clock & Status, Widget bawaan & `AppWidgetHost`, Folder interaktif, Dock mengambang, dan pencarian universal.
- App Drawer dengan pencarian, filter kategori, pengurutan, dan dua cara akses yang dapat dikonfigurasi (Swipe Up & Tombol App Drawer).
- Control Center Liquid Glass terintegrasi dengan API resmi Android (Wi-Fi panel, Bluetooth, Senter, Kecerahan, Volume Media/Dering, Default Launcher).
- Personalisasi tampilan (Light/Dark/System, wallpaper + Photo Picker, transparansi, blur, grid, ukuran ikon, animasi, gestur).
- Lima kapabilitas wajib AI Rulebook V3: Permission Management, Helper Separation, Crash Recovery (`Salin Log` / `Kembali`), GitHub Release Updater (`Skip` / `Nanti` / `Update`), dan Structured Private Storage (`H P i O/Pembaruan Aplikasi/` & `H P i O/Log Crash/`).
- Pipeline GitHub Actions (`build-release.yml`) dengan validasi paket, lint, unit test, signing `ANDIKUNE_*`, verifikasi sertifikat/signature, publikasi release, dan cleanup 3 artifact.
