package id.jasmanipro.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.jasmanipro.app.AppViewModel
import id.jasmanipro.app.BuildConfig
import id.jasmanipro.app.data.AppSettings
import id.jasmanipro.app.data.ThemeMode
import id.jasmanipro.app.ui.InfoCard
import id.jasmanipro.app.ui.ScreenHeader
import id.jasmanipro.domain.calc.TieBreak

@Composable
fun SettingsScreen(vm: AppViewModel, settings: AppSettings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Pengaturan")

        InfoCard {
            CardTitle("Tema")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    onClick = { vm.setTheme(ThemeMode.SYSTEM) },
                    label = { Text("Ikuti sistem") },
                )
                FilterChip(
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    onClick = { vm.setTheme(ThemeMode.LIGHT) },
                    label = { Text("Terang") },
                )
                FilterChip(
                    selected = settings.themeMode == ThemeMode.DARK,
                    onClick = { vm.setTheme(ThemeMode.DARK) },
                    label = { Text("Gelap") },
                )
            }
        }

        InfoCard {
            CardTitle("Hasil yang cocok dengan beberapa nilai")
            Text(
                "Pada tabel estimasi, beberapa baris nilai bisa punya ambang yang sama. Pilih nilai yang " +
                    "ditampilkan. Aturan resmi belum dikonfirmasi, jadi rentangnya selalu ditampilkan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.tieBreak == TieBreak.HIGHEST_SCORE,
                    onClick = { vm.setTieBreak(TieBreak.HIGHEST_SCORE) },
                    label = { Text("Nilai tertinggi") },
                )
                FilterChip(
                    selected = settings.tieBreak == TieBreak.LOWEST_SCORE,
                    onClick = { vm.setTieBreak(TieBreak.LOWEST_SCORE) },
                    label = { Text("Nilai terendah") },
                )
            }
        }

        InfoCard {
            CardTitle("Panjang lintasan lari")
            Text(
                "Dipakai untuk mengubah jumlah putaran menjadi meter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(200, 250, 300, 400).forEach { meters ->
                    FilterChip(
                        selected = settings.trackLengthMeters == meters,
                        onClick = { vm.setTrackLength(meters) },
                        label = { Text("$meters m") },
                    )
                }
            }
        }

        InfoCard {
            CardTitle("Privasi")
            Text(
                "Aplikasi ini tidak meminta izin apa pun dan tidak mengirim data ke server. Versi ini belum " +
                    "menyimpan data peserta. Yang tersimpan di perangkat hanya pengaturan di halaman ini.",
            )
        }

        InfoCard {
            CardTitle("Tentang")
            Text("Jasmani Pro versi ${BuildConfig.VERSION_NAME}")
            val bundle = vm.bundle
            if (bundle != null) {
                Text(
                    "Sumber norma: ${bundle.sourceFileName}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Sidik berkas (SHA-256): ${bundle.sourceSha256.take(16)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Identitas visual dibuat sendiri dan tidak berafiliasi dengan instansi mana pun. " +
                    "Hasil perhitungan bukan nilai resmi selama norma belum dikonfirmasi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}
