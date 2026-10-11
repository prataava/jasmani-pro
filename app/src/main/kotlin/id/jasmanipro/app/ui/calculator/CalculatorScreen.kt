package id.jasmanipro.app.ui.calculator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.jasmanipro.app.AppViewModel
import id.jasmanipro.app.data.AppSettings
import id.jasmanipro.app.ui.EmptyState
import id.jasmanipro.app.ui.InfoCard
import id.jasmanipro.app.ui.ScreenHeader
import id.jasmanipro.app.ui.SectionTitle
import id.jasmanipro.app.ui.StatusChip
import id.jasmanipro.app.ui.fmt
import id.jasmanipro.app.ui.label
import id.jasmanipro.app.ui.theme.StatusGreen
import id.jasmanipro.app.ui.theme.StatusRed
import id.jasmanipro.app.ui.theme.StatusYellow
import id.jasmanipro.domain.calc.ComponentScore
import id.jasmanipro.domain.calc.FormEvaluation
import id.jasmanipro.domain.calc.FormulaConfig
import id.jasmanipro.domain.calc.LariMode
import id.jasmanipro.domain.calc.RangeFlag
import id.jasmanipro.domain.calc.ResultLabel
import id.jasmanipro.domain.calc.TargetPlanner
import id.jasmanipro.domain.input.ParseResult
import id.jasmanipro.domain.model.Applicability
import id.jasmanipro.domain.model.ComponentGroup
import id.jasmanipro.domain.model.ComponentId
import id.jasmanipro.domain.model.Direction
import id.jasmanipro.domain.model.Gender
import id.jasmanipro.domain.model.NormComponent
import id.jasmanipro.domain.model.NormTable
import id.jasmanipro.domain.model.RowStatus

@Composable
fun CalculatorScreen(vm: AppViewModel, settings: AppSettings) {
    val table = vm.table
    val eval = vm.evaluate(settings)
    if (table == null || eval == null) {
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
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Hitung nilai", "Isi hasil tes. Kolom kosong tidak dihitung sebagai nol.")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = vm.gender == Gender.PRIA,
                onClick = { vm.gender = Gender.PRIA },
                label = { Text("Norma Pria") },
            )
            FilterChip(
                selected = vm.gender == Gender.WANITA,
                onClick = { vm.gender = Gender.WANITA },
                label = { Text("Norma Wanita") },
            )
        }

        if (table.metadata.applicability != Applicability.CONFIRMED) {
            InfoCard {
                Text(
                    "Norma ini belum dikonfirmasi berlaku untuk seleksi atau periode tertentu. " +
                        "Semua nilai di bawah adalah ESTIMASI.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        ComponentGroup.entries.forEach { group ->
            val items = table.components.filter { it.group == group }
            if (items.isNotEmpty()) {
                SectionTitle(
                    when (group) {
                        ComponentGroup.A -> "Kesamaptaan A"
                        ComponentGroup.B -> "Kesamaptaan B"
                        ComponentGroup.C -> "Kesamaptaan C"
                    },
                )
                items.forEach { c ->
                    if (c.id == ComponentId.LARI_12M) {
                        LariCard(vm, c, eval, settings)
                    } else {
                        ComponentCard(vm, c, eval)
                    }
                }
            }
        }

        SectionTitle("Hasil")
        ResultCard(eval)
        TargetCard(vm, table, eval)

        OutlinedButton(onClick = { vm.clearForm() }, modifier = Modifier.fillMaxWidth()) {
            Text("Kosongkan semua isian")
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    decimal: Boolean,
    isError: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 12) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FeedbackLine(parse: ParseResult?) {
    when (parse) {
        is ParseResult.Invalid -> Text(
            parse.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        is ParseResult.Valid -> parse.warnings.forEach {
            Text(
                "Perhatian: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> Unit
    }
}

@Composable
private fun ComponentCard(vm: AppViewModel, c: NormComponent, eval: FormEvaluation) {
    val parse = eval.fields[c.id]
    InfoCard {
        Text(c.id.label(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        c.unitNote?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NumberField(
            value = vm.texts[c.id].orEmpty(),
            onChange = { vm.texts[c.id] = it },
            label = "${c.id.label()} (${c.unit})",
            decimal = c.unit == "detik",
            isError = parse is ParseResult.Invalid,
        )
        FeedbackLine(parse)
        eval.result.components[c.id]?.let { ScoreLine(it) }
    }
}

@Composable
private fun LariCard(vm: AppViewModel, c: NormComponent, eval: FormEvaluation, settings: AppSettings) {
    val parse = eval.fields[c.id]
    val meters = eval.lariMeters
    InfoCard {
        Text(c.id.label(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = vm.lariMode == LariMode.METER,
                onClick = { vm.lariMode = LariMode.METER },
                label = { Text("Meter") },
            )
            FilterChip(
                selected = vm.lariMode == LariMode.KILOMETER,
                onClick = { vm.lariMode = LariMode.KILOMETER },
                label = { Text("Kilometer") },
            )
            FilterChip(
                selected = vm.lariMode == LariMode.LAPS,
                onClick = { vm.lariMode = LariMode.LAPS },
                label = { Text("Putaran") },
            )
        }
        val isError = parse is ParseResult.Invalid
        when (vm.lariMode) {
            LariMode.METER -> NumberField(
                value = vm.texts[c.id].orEmpty(),
                onChange = { vm.texts[c.id] = it },
                label = "Jarak (meter)",
                decimal = true,
                isError = isError,
            )
            LariMode.KILOMETER -> NumberField(
                value = vm.texts[c.id].orEmpty(),
                onChange = { vm.texts[c.id] = it },
                label = "Jarak (kilometer)",
                decimal = true,
                isError = isError,
            )
            LariMode.LAPS -> {
                NumberField(
                    value = vm.lapsText,
                    onChange = { vm.lapsText = it },
                    label = "Jumlah putaran penuh",
                    decimal = false,
                    isError = isError,
                )
                NumberField(
                    value = vm.remainderText,
                    onChange = { vm.remainderText = it },
                    label = "Sisa jarak (meter)",
                    decimal = true,
                    isError = isError,
                )
                Text(
                    "Panjang lintasan ${settings.trackLengthMeters} m. Ubah di tab Pengaturan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FeedbackLine(parse)
        if (vm.lariMode != LariMode.METER && meters != null) {
            Text("Setara ${fmt(meters)} meter", style = MaterialTheme.typography.bodyMedium)
        }
        eval.result.components[c.id]?.let { ScoreLine(it) }
    }
}

@Composable
private fun ScoreLine(s: ComponentScore) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nilai ${s.score}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            when (s.matchedStatus) {
                RowStatus.VERIFIED -> StatusChip("Baris terverifikasi", StatusGreen)
                RowStatus.LINEAR_ESTIMATE -> StatusChip("Estimasi linier", StatusYellow)
                null -> StatusChip("Di bawah batas tabel", StatusRed)
            }
        }
        s.tiedScoreRange?.let {
            Text(
                "Hasil ini cocok dengan nilai ${it.first} sampai ${it.last} (ambang sama di tabel). " +
                    "Yang ditampilkan: ${s.score}. Aturan bisa diubah di Pengaturan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (s.flag) {
            RangeFlag.ABOVE_TABLE_MAX -> Text(
                "Melebihi ambang nilai 100 pada tabel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RangeFlag.BELOW_TABLE_MIN -> Text(
                "Di bawah ambang nilai 0 pada tabel, diberi nilai 0.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RangeFlag.NONE -> Unit
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ResultCard(eval: FormEvaluation) {
    val r = eval.result
    InfoCard {
        if (r.components.isEmpty()) {
            Text("Belum ada hasil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Isi minimal satu komponen di atas. Nilai akan muncul otomatis.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val official = r.label == ResultLabel.OFFICIAL
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ringkasan nilai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (official) StatusChip("RESMI", StatusGreen) else StatusChip("ESTIMASI", StatusYellow)
            }
            r.labelReasons.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ResultRow("Nilai A (lari)", r.scoreA?.let { fmt(it) } ?: "-")
            ResultRow("Rata-rata B", r.averageB?.let { fmt(it) } ?: "belum lengkap")
            ResultRow("Nilai C (renang)", r.scoreC?.let { fmt(it) } ?: "-")
            ResultRow("Gabungan A dan B", r.combinedAB?.let { fmt(it) } ?: "belum lengkap")
            Text(
                "Rumus gabungan: ${FormulaConfig.ESTIMATE_AB.name}. ${FormulaConfig.ESTIMATE_AB.sourceNote}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ResultRow(
                "Nilai akhir dengan C",
                r.finalWithC?.let { fmt(it) } ?: "belum dihitung (bobot C belum dikonfirmasi)",
            )
            if (r.missing.isNotEmpty()) {
                Text(
                    "Belum diisi: " + r.missing.joinToString { it.label() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (eval.hasInvalid) {
            Text(
                "Ada isian yang tidak valid. Kolom itu tidak ikut dihitung sampai diperbaiki.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TargetCard(vm: AppViewModel, table: NormTable, eval: FormEvaluation) {
    InfoCard {
        Text("Target latihan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Pilih nilai yang ingin dicapai. Ini target latihan, bukan prediksi nilai resmi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(60, 70, 80, 90, 100).forEach { target ->
                FilterChip(
                    selected = vm.targetScore == target,
                    onClick = { vm.targetScore = target },
                    label = { Text("$target") },
                )
            }
        }
        val scored = eval.result.components.values.toList()
        if (scored.isEmpty()) {
            Text("Isi hasil tes dulu untuk melihat selisih ke target.")
        } else {
            scored.forEach { s ->
                val comp = table.component(s.id)
                val threshold = TargetPlanner.thresholdFor(table, s.id, vm.targetScore)
                val gap = TargetPlanner.gapToTarget(table, s.id, s.raw, vm.targetScore)
                if (comp != null && threshold != null && gap != null) {
                    val status = when {
                        gap <= 0.0 -> "sudah tercapai"
                        comp.direction == Direction.LOWER_IS_BETTER -> "perlu lebih cepat ${fmt(gap)} ${comp.unit}"
                        else -> "perlu tambah ${fmt(gap)} ${comp.unit}"
                    }
                    Text("${s.id.label()}: target ${fmt(threshold)} ${comp.unit}, $status")
                }
            }
        }
    }
}
