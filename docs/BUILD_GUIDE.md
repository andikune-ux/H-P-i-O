# Build & Signing Guide — H P i O

## 1. Identitas Aplikasi & Signing Andik Une
- **Application ID / Namespace**: `dev.andikune.launcherhpio`
- **CN**: `Andik Une`
- **Organization**: `Andik Une`
- **Alias Keystore**: `andikune`

## 2. Pembuatan Release Keystore Konsisten & Upload Otomatis ke GitHub Secrets
Agar pembaruan APK (**H P i O**) selalu dapat dipasang di atas versi lama tanpa error bentrok tanda tangan (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), gunakan **satu keystore yang sama** untuk seluruh rilis.

### Opsi Otomatis (1 Perintah — Tanpa Input Manual)
Jika memiliki GitHub CLI (`gh`) yang sudah login (`gh auth login`), jalankan script berikut sekali dari root proyek:

```bash
bash scripts/setup-andikune-secrets.sh andikune-ux/H-P-i-O
```

Script tersebut otomatis membuat keystore konsisten (`~/.andikune-keys/hpio-andikune-release.jks`, alias `andikune`, `CN=Andik Une, O=Andik Une, C=ID`) dengan password acak aman, mengekstrak `SHA-256`, dan mengunggah seluruh 5 secrets (`ANDIKUNE_KEYSTORE_BASE64`, `ANDIKUNE_KEYSTORE_PASSWORD`, `ANDIKUNE_KEY_ALIAS`, `ANDIKUNE_KEY_PASSWORD`, `ANDIKUNE_CERT_SHA256`) langsung ke GitHub tanpa menyimpan key ke dalam repository.

### Opsi Manual (Jika Menggunakan GitHub Web UI)
Jika belum memiliki file keystore rilis Andik Une, jalankan sekali di komputer/termux pribadi Anda (jangan pernah simpan file `.jks` di repository):

```bash
keytool -genkeypair -v \
  -keystore andikune-release.jks \
  -alias andikune \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Andik Une, O=Andik Une, C=ID"
```

Catat **SHA-256 certificate fingerprint** dari sertifikat tersebut:

```bash
keytool -list -v -keystore andikune-release.jks -alias andikune | grep SHA256
```

Encode ke Base64 satu baris untuk GitHub Secret:

```bash
base64 -w 0 andikune-release.jks > keystore.b64
```

**Simpan backup `andikune-release.jks` dan passwordnya di tempat aman. Jangan pernah membuat ulang keystore baru untuk rilis berikutnya.**

## 3. Konfigurasi GitHub Secrets (`andikune-ux/H-P-i-O`)
Buka `Settings → Secrets and variables → Actions → New repository secret` dan isi:
- `ANDIKUNE_KEYSTORE_BASE64` — isi file `keystore.b64` (1 baris Base64)
- `ANDIKUNE_KEYSTORE_PASSWORD` — password keystore
- `ANDIKUNE_KEY_ALIAS` — `andikune`
- `ANDIKUNE_KEY_PASSWORD` — password key
- `ANDIKUNE_CERT_SHA256` *(opsional, sangat disarankan)* — SHA-256 fingerprint sertifikat (tanpa atau dengan `:`) untuk mengunci agar CI selalu menolak build jika kunci berubah.

Selain pinning opsional di atas, workflow `.github/workflows/build-release.yml` otomatis membandingkan sertifikat build baru terhadap rilis GitHub terakhir sebelum mempublikasikan rilis baru.

## 4. Gradle Wrapper
`gradlew`, `gradlew.bat`, dan `gradle/wrapper/gradle-wrapper.jar` (Gradle `9.3.1`) sudah disertakan di proyek sehingga GitHub Actions langsung mengeksekusi `./gradlew` tanpa setup manual tambahan.
