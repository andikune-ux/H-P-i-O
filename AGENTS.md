# AGENTS.md — H P i O Project Rules

Proyek **H P i O** (`dev.andikune.launcherhpio`) mengikuti **AI Rulebook V3** (`https://github.com/andikune-ux/ai-rulebook`, Commit SHA: `4133a27721a40ab356ac46f0525c450cb63c5f7c`) beserta spesifikasi produk dan identitas Android berikut.

## 1. Aturan Wajib AI Rulebook V3
- Baca dan patuhi seluruh aturan universal di `core/PRINCIPLES.md`, `core/AI_BEHAVIOR.md`, `core/CODING_STANDARDS.md`, `core/SECURITY_RULES.md`, `core/TESTING_RULES.md`, `core/DATABASE_RULES.md`, `core/RELEASE_POLICY.md`, `core/RISK_POLICY.md`, `core/MANDATORY_APP_CAPABILITIES.md`, `core/BUGFIX_WORKFLOW.md`, dan `platforms/android/RULES.md`.
- Terapkan Risk-Based Approval (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). Pembuatan atau penggantian keystore signing adalah tindakan **HIGH** yang wajib persetujuan eksplisit pemilik (**Andik Une**).
- Kerjakan perbaikan bug satu fitur pada satu waktu sesuai `core/BUGFIX_WORKFLOW.md`.
- Laporkan hasil verifikasi dengan status `PASS`, `FAIL`, `NOT RUN`, atau `NOT APPLICABLE` berdasarkan bukti aktual.

## 2. Identitas Paket Mutlak H P i O
- **Application ID**: `dev.andikune.launcherhpio`
- **Namespace**: `dev.andikune.launcherhpio`
- **Root Kotlin Package**: `dev.andikune.launcherhpio`
- Dilarang keras menggunakan `com.example` di source code, manifest, Gradle, maupun test.

## 3. Versioning H P i O
- Proyek **H P i O** **tidak** menggunakan format versi NSiO (`v.{MAJOR}.{DAY}.{COMMIT_NUMBER}`).
- H P i O menggunakan Semantic Versioning (`MAJOR.MINOR.PATCH` di `version.properties`, tag Git `v{MAJOR}.{MINOR}.{PATCH}`) dan `versionCode` bilangan bulat yang meningkat secara monoton.

## 4. Signing & Secrets Andik Une
- Identitas sertifikat rilis: `CN=Andik Une`, `O=Andik Une`, alias `andikune`.
- Gunakan hanya GitHub Secrets: `ANDIKUNE_KEYSTORE_BASE64`, `ANDIKUNE_KEYSTORE_PASSWORD`, `ANDIKUNE_KEY_ALIAS`, `ANDIKUNE_KEY_PASSWORD`.
- Dilarang commit file keystore, Base64 keystore, password, atau mencetak nilai secret ke log.

## 5. Lima Kapabilitas Wajib
1. **Permission & Capability Management** — least privilege, penjelas tujuan, tangani granted/denied/permanently denied/revoked/unavailable.
2. **Service / Helper Separation** — helper/repository terpisah tanpa file service kosong palsu.
3. **Crash Recovery** — tangkap exception tanpa memblokir handler default, sensor secret, tampilkan pada launch berikutnya dengan tombol **Salin Log** & **Kembali**, cegah crash loop.
4. **GitHub Release Update Notification** — cek asinkron `andikune-ux/H-P-i-O` tanpa token privat, tombol **Skip**, **Nanti**, **Update**, verifikasi integritas & izin instalasi resmi Android.
5. **Structured Private Storage** — `filesDir/H P i O/Pembaruan Aplikasi/` dan `filesDir/H P i O/Log Crash/` dengan write atomik, validasi nama file, dan retensi terbatas.
