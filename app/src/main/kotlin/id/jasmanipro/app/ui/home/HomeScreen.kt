package id.jasmanipro.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.jasmanipro.app.AppViewModel
import id.jasmanipro.app.ui.EmptyState
import id.jasmanipro.app.ui.InfoCard
import id.jasmanipro.app.ui.ScreenHeader
import id.jasmanipro.domain.model.RowStatus

@Composable
fun HomeScreen(vm: AppViewModel, onStart: () -> Unit) {
    val bundle = vm.bundle
    if (bundle == null) {
        EmptyState(
            title = "Norma tidak dapat dimuat",
            message = vm.loadError ?: "Berkas norma bawaan rusak. Pasang ulang aplikasi.",
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Jasmani Pro", "Kalkulator nilai kesamaptaan jasmani")

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text("Mulai hitung nilai")
        }

        InfoCard {
            Text("Cara pakai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("1. Buka tab Hitung dan pilih norma Pria atau Wanita.")
            Text("2. Isi hasil tes. Kolom yang kosong tidak dihitung sebagai nol.")
            Text("3. Nilai muncul otomatis, lengkap dengan status norma yang dipakai.")
            Text("4. Pilih target latihan untuk melihat selisih yang harus dikejar.")
        }

        InfoCard {
            Text(
                "Status norma yang dipakai",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            bundle.tables.forEach { t ->
                val verified = t.rows.count { it.status == RowStatus.VERIFIED }
                Text("${t.metadata.category}: $verified dari ${t.rows.size} baris terverifikasi, sisanya estimasi linier.")
            }
            Text(
                "Norma ini BELUM dikonfirmasi berlaku untuk seleksi atau periode tertentu, jadi semua hasil " +
                    "berlabel ESTIMASI. Rumus nilai gabungan juga belum dikonfirmasi. Lihat rinciannya di tab Norma.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        InfoCard {
            Text("Status versi ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Sudah ada: hitung nilai, lihat tabel norma, pengaturan.")
            Text(
                "Belum ada (dalam pengembangan): profil peserta, riwayat, ekspor Excel, CSV, PDF, serta " +
                    "backup. Hasil di tab Hitung belum disimpan dan hilang bila aplikasi ditutup.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
