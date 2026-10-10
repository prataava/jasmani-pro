# Audit Norma — Tabel_Nilai_Jasmani_Polri_2026-v2.xlsx

Tanggal audit: 10 Oktober 2026. Hasil ini dibuat oleh `tools/convert_norma.py` dan diperiksa ulang oleh `tools/reference_check.py`.

## Struktur
- 3 sheet: Norma Pria, Norma Wanita, Log Pengujian Batas. Tidak ada formula, komentar, atau grafik.
- Tiap norma: 101 baris (nilai 0–100), tanpa duplikat atau sel kosong, semua numerik.
- Arah: lari, pull-up/chinning, sit-up, push-up = makin besar makin baik; shuttle run, renang = makin kecil makin baik.

## Status verifikasi (per baris, dipertahankan apa adanya)
| Norma | Terverifikasi (Kep/1352/VI/2020) | Estimasi linier |
|---|---|---|
| Pria | nilai 100, 41, 0 | 98 baris |
| Wanita | nilai 100 | 100 baris |

Status di workbook tidak membuktikan norma berlaku untuk seluruh seleksi Akpol 2026/2027. Karena itu `applicability = UNCONFIRMED` untuk kedua tabel, dan metadata yang tidak ada di workbook (nama dokumen, tanggal dokumen, periode berlaku) diisi `null`.

## Temuan terbuka
1. **Chinning wanita** berlabel "repetisi" (dikonfirmasi pengguna), tetapi nilai 72 pada nilai 100 tampak tidak lazim. Belum diverifikasi terhadap dokumen resmi. Nilai 0–40 semuanya chinning 0.
2. **Ambang kembar.** Estimasi linier yang dibulatkan membuat banyak baris berambang sama (pull-up pria 17 untuk nilai 95–100; push-up wanita 0 untuk nilai 0–38; sit-up wanita 0 untuk nilai 0–37). Aturan pemilihan nilai (`TieBreak`) dibuat dapat dikonfigurasi; default `HIGHEST_SCORE`. Hasil ambigu selalu ditandai dengan rentang nilai.
3. **Log Pengujian Batas**: 11 entri, semuanya cocok dengan tabel, tetapi hanya menguji nilai 100, tidak ada entri Wanita–Renang, dan sumber buktinya ("Blangko TKJ Polri", "Standar SSDM Polri") berbeda dari rujukan di sheet norma.
4. **Rumus nilai gabungan dan bobot C tidak ada** di workbook. Kalkulator memakai `FormulaConfig.ESTIMATE_AB` (rata-rata A dan B bobot sama, belum dikonfirmasi) dan tidak menghitung nilai akhir dengan C sampai bobotnya diberikan.
5. Aturan pembulatan belum dikonfirmasi. Bawaan: setengah ke atas, 2 desimal, hanya di akhir.

## Pemeriksaan yang sudah dijalankan
- Struktur JSON hasil konversi cocok dengan DTO Kotlin.
- Algoritma yang sama dijalankan dari JSON terhadap 2.288 vektor uji turunan Excel: 0 selisih.
- Kode Kotlin BELUM dikompilasi dan tesnya BELUM dijalankan (tidak ada kompiler Kotlin di lingkungan penyusun). Verifikasi nyata menunggu build di GitHub Actions.
