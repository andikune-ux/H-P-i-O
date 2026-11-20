# Release Guide — H P i O

## Skema Versi H P i O
- `version.properties` menyimpan `VERSION_MAJOR`, `VERSION_MINOR`, `VERSION_PATCH`, dan `VERSION_CODE_OFFSET`.
- `scripts/generate-version.sh` menghasilkan:
  - `versionName`: `{MAJOR}.{MINOR}.{PATCH}` (contoh `1.0.0`)
  - `tag`: `v{MAJOR}.{MINOR}.{PATCH}` (contoh `v1.0.0`)
  - `versionCode`: `VERSION_CODE_OFFSET + GITHUB_RUN_NUMBER` (monotonically increasing integer).

## Checklist Sebelum Release
1. Perbarui `VERSION_MAJOR`/`VERSION_MINOR`/`VERSION_PATCH` di `version.properties` dan catat di `CHANGELOG.md`.
2. Pastikan seluruh validasi `validate-repository` dan `test-and-lint` berstatus **PASS**.
3. Pastikan `build-signed-release` menandatangani APK menggunakan `ANDIKUNE_*` secrets, memverifikasi signature dengan `apksigner verify --print-certs`, dan menghasilkan `.sha256`.
4. `publish-release` hanya berjalan dari branch `main` setelah seluruh validasi lolos dan tidak menimpa tag yang sudah ada.
5. Jangan menghapus release terakhir yang valid (`last known-good release`); cleanup hanya mempertahankan 3 workflow artifact terbaru.
