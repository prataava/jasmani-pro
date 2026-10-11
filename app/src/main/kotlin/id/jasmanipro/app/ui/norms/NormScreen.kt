package id.jasmanipro.app.ui.norms

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.jasmanipro.app.AppViewModel
import id.jasmanipro.app.ui.EmptyState
import id.jasmanipro.app.ui.InfoCard
import id.jasmanipro.app.ui.ScreenHeader
import id.jasmanipro.app.ui.fmt
import id.jasmanipro.app.ui.label
import id.jasmanipro.app.ui.theme.StatusGreen
import id.jasmanipro.domain.model.Applicability
import id.jasmanipro.domain.model.Gender
import id.jasmanipro.domain.model.NormTable
import id.jasmanipro.domain.model.RowStatus

private val ScoreWidth = 56.dp
private val ValueWidth = 84.dp
private val StatusWidth = 124.dp

@Composable
fun NormScreen(vm: AppViewModel) {
    val bundle = vm.bundle
    if (bundle == null) {
        EmptyState(
            title = "Norma tidak dapat dimuat",
            message = vm.loadError ?: "Berkas norma bawaan rusak. Pasang ulang aplikasi.",
        )
        return
    }
    var gender by rememberSaveable { mutableStateOf(Gender.PRIA) }
    val table = bundle.tableFor(gender)
    val hScroll = rememberScrollState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Norma", "Tabel nilai, sumber, dan status verifikasinya.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = gender == Gender.PRIA,
                    onClick = { gender = Gender.PRIA },
                    label = { Text("Pria") },
                )
                FilterChip(
                    selected = gender == Gender.WANITA,
                    onClick = { gender = Gender.WANITA },
                    label = { Text("Wanita") },
                )
            }
        }
        if (table == null) {
            item { Text("Tabel norma untuk pilihan ini tidak tersedia.") }
            return@LazyColumn
        }
        item { MetadataCard(table) }
        item {
            Text(
                "Geser tabel ke samping untuk melihat semua kolom. Baris hijau terverifikasi, " +
                    "baris lain estimasi linier.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(hScroll)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderCell("Nilai", ScoreWidth)
                table.components.forEach { c -> HeaderCell("${c.id.label()}\n(${c.unit})", ValueWidth) }
                HeaderCell("Status", StatusWidth)
            }
        }
        itemsIndexed(table.rows, key = { _, row -> row.score }) { index, row ->
            val verified = row.status == RowStatus.VERIFIED
            val bg = when {
                verified -> StatusGreen.copy(alpha = 0.22f)
                index % 2 == 0 -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(hScroll)
                    .background(bg)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BodyCell("${row.score}", ScoreWidth, bold = true)
                table.components.forEach { c ->
                    BodyCell(fmt(row.thresholds.getValue(c.id)), ValueWidth)
                }
                BodyCell(if (verified) "Terverifikasi" else "Estimasi", StatusWidth)
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 3,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun BodyCell(text: String, width: androidx.compose.ui.unit.Dp, bold: Boolean = false) {
    Text(
        text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun MetadataCard(table: NormTable) {
    val m = table.metadata
    val unknown = "Belum diketahui"
    InfoCard {
        Text("Sumber dan status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        MetaRow("Sumber", m.sourceName)
        MetaRow("Nama dokumen", m.documentName ?: unknown)
        MetaRow("Nomor keputusan", m.decisionNumber ?: "$unknown (rujukan Kep/1352/VI/2020 hanya pada baris terverifikasi)")
        MetaRow("Tanggal dokumen", m.documentDate ?: unknown)
        MetaRow("Periode berlaku", m.validPeriod ?: unknown)
        MetaRow("Kategori", m.category)
        MetaRow(
            "Berlaku untuk seleksi",
            if (m.applicability == Applicability.CONFIRMED) {
                "Dikonfirmasi"
            } else {
                "BELUM dikonfirmasi untuk seleksi atau periode tertentu"
            },
        )
        MetaRow("Catatan verifikasi", m.verificationNote)
        table.components.forEach { c ->
            c.unitNote?.let { MetaRow("Catatan ${c.id.label()}", it) }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
