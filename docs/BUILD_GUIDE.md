# Build & Signing Guide — H P i O

## 1. Identitas Aplikasi & Signing Andik Une
- **Application ID / Namespace**: `dev.andikune.launcherhpio`
- **CN**: `Andik Une`
- **Organization**: `Andik Une`
- **Alias Keystore**: `andikune`

## 2. Prosedur Pembuatan Release Keystore Pertama (HIGH RISK — Memerlukan Eksekusi/Persetujuan Pemilik)
Jika belum memiliki file keystore rilis Andik Une, jalankan perintah berikut di mesin lokal yang aman (jangan pernah di repository atau log CI):

```bash
keytool -genkeypair -v \
  -keystore andikune-release.jks \
  -alias andikune \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Andik Une, O=Andik Une, C=ID"
```

Verifikasi subject dan SHA-256 fingerprint sertifikat:

```bash
keytool -list -v -keystore andikune-release.jks -alias andikune
```

Encode ke Base64 tanpa line-wrapping untuk GitHub Secret:

```bash
base64 -w 0 andikune-release.jks > keystore.b64
```

**Simpan backup `andikune-release.jks` dan passwordnya di password manager terenkripsi milik pemilik. Jangan pernah mengganti keystore setelah rilis pertama agar pembaruan aplikasi (upgrade) tetap kompatibel.**

## 3. Konfigurasi GitHub Secrets (`andikune-ux/H-P-i-O`)
Buka `Settings → Secrets and variables → Actions → New repository secret` dan tambahkan keempat secret berikut:
- `ANDIKUNE_KEYSTORE_BASE64`
- `ANDIKUNE_KEYSTORE_PASSWORD`
- `ANDIKUNE_KEY_ALIAS` (nilai: `andikune`)
- `ANDIKUNE_KEY_PASSWORD`

## 4. Gradle Wrapper
`gradlew`, `gradlew.bat`, dan `gradle/wrapper/gradle-wrapper.jar` (Gradle `9.3.1`) sudah disertakan di proyek agar GitHub Actions dapat langsung membangun APK tanpa setup manual tambahan. Pastikan izin eksekusi `gradlew` tetap aktif (`git update-index --chmod=+x gradlew` bila diperlukan).
