package id.jasmanipro.domain.calc

import id.jasmanipro.domain.model.Applicability
import id.jasmanipro.domain.model.ComponentGroup
import id.jasmanipro.domain.model.ComponentId
import id.jasmanipro.domain.model.Direction
import id.jasmanipro.domain.model.NormRow
import id.jasmanipro.domain.model.NormTable
import id.jasmanipro.domain.model.RowStatus
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Aturan bila satu hasil cocok dengan beberapa baris norma yang ambangnya sama
 * (terjadi karena estimasi linier dibulatkan, mis. pull-up pria 17 untuk nilai 95-100).
 * Aturan mana yang berlaku di dokumen resmi belum dikonfirmasi, jadi dibuat dapat dikonfigurasi.
 */
enum class TieBreak { HIGHEST_SCORE, LOWEST_SCORE }

/**
 * Konfigurasi rumus nilai gabungan. Semua angka di sini adalah KONFIGURASI, bukan rumus resmi,
 * kecuali [confirmed] bernilai true. Bobot bersifat relatif (tidak harus berjumlah 1).
 *
 * [weightC] null berarti pembobotan Kesamaptaan C belum dikonfirmasi, sehingga nilai akhir
 * dengan C tidak dihitung sama sekali.
 */
data class FormulaConfig(
    val id: String,
    val name: String,
    val weightA: Double,
    val weightB: Double,
    val weightC: Double?,
    val confirmed: Boolean,
    val sourceNote: String,
    val tieBreak: TieBreak = TieBreak.HIGHEST_SCORE,
    val decimals: Int = 2,
) {
    init {
        require(weightA >= 0 && weightB >= 0 && (weightC ?: 0.0) >= 0) { "Bobot tidak boleh negatif." }
        require(weightA + weightB > 0) { "Bobot A dan B tidak boleh sama-sama nol." }
        require(decimals in 0..6) { "Jumlah desimal harus 0..6." }
    }

    companion object {
        /** Bawaan: rata-rata A dan B bobot sama. Belum dikonfirmasi, selalu berlabel estimasi. */
        val ESTIMATE_AB = FormulaConfig(
            id = "estimasi_ab",
            name = "Estimasi: rata-rata A dan B (bobot sama)",
            weightA = 1.0,
            weightB = 1.0,
            weightC = null,
            confirmed = false,
            sourceNote = "Bobot A, B, dan C serta aturan pembulatan belum dikonfirmasi dari dokumen resmi.",
        )
    }
}

enum class RangeFlag { NONE, ABOVE_TABLE_MAX, BELOW_TABLE_MIN }

data class ComponentScore(
    val id: ComponentId,
    val raw: Double,
    val unit: String,
    val score: Int,
    /** Status baris norma yang dipakai; null bila hasil di bawah batas tabel. */
    val matchedStatus: RowStatus?,
    /** Rentang nilai yang cocok bila ambang kembar (hasil ambigu); null bila tidak ambigu. */
    val tiedScoreRange: IntRange?,
    val flag: RangeFlag,
)

enum class ResultLabel { OFFICIAL, ESTIMATE }

data class CalculationResult(
    val components: Map<ComponentId, ComponentScore>,
    val missing: List<ComponentId>,
    val scoreA: Double?,
    val averageB: Double?,
    val scoreC: Double?,
    val combinedAB: Double?,
    /** Nilai akhir dengan bobot C; null bila C kosong atau bobot C belum dikonfirmasi. */
    val finalWithC: Double?,
    val label: ResultLabel,
    val labelReasons: List<String>,
    val formulaId: String,
) {
    val isComplete: Boolean get() = missing.isEmpty()
}

object NormCalculator {

    /**
     * Nilai satu komponen. Aturan: baris dengan nilai tertinggi yang ambangnya terpenuhi
     * (lari/repetisi: hasil >= ambang; waktu: hasil <= ambang), lalu [tieBreak] bila ambangnya kembar.
     * Hasil di bawah ambang nilai 0 diberi nilai 0 dan ditandai [RangeFlag.BELOW_TABLE_MIN].
     */
    fun scoreComponent(
        table: NormTable,
        id: ComponentId,
        raw: Double,
        tieBreak: TieBreak = TieBreak.HIGHEST_SCORE,
    ): ComponentScore {
        require(raw.isFinite() && raw >= 0.0) { "Hasil harus bilangan tidak negatif." }
        val comp = requireNotNull(table.component(id)) { "Komponen $id tidak ada pada norma '${table.id}'." }
        val rows = table.rows
        val matchIdx = rows.indexOfFirst { meets(comp.direction, raw, it.thresholds.getValue(id)) }
        if (matchIdx < 0) {
            return ComponentScore(id, raw, comp.unit, 0, null, null, RangeFlag.BELOW_TABLE_MIN)
        }
        val matched = rows[matchIdx]
        val threshold = matched.thresholds.getValue(id)
        val tied: List<NormRow> = rows.filter { it.thresholds.getValue(id) == threshold }
        val chosen = when (tieBreak) {
            TieBreak.HIGHEST_SCORE -> matched
            TieBreak.LOWEST_SCORE -> tied.minByOrNull { it.score } ?: matched
        }
        val range = if (tied.size > 1) tied.minOf { it.score }..tied.maxOf { it.score } else null
        val beyondTop = matchIdx == 0 && when (comp.direction) {
            Direction.HIGHER_IS_BETTER -> raw > threshold
            Direction.LOWER_IS_BETTER -> raw < threshold
        }
        return ComponentScore(
            id = id,
            raw = raw,
            unit = comp.unit,
            score = chosen.score,
            matchedStatus = chosen.status,
            tiedScoreRange = range,
            flag = if (beyondTop) RangeFlag.ABOVE_TABLE_MAX else RangeFlag.NONE,
        )
    }

    /**
     * Hitung seluruh nilai. Input null berarti belum diisi dan TIDAK dianggap nol.
     * Rata-rata B dan nilai gabungan hanya dihitung bila seluruh komponen yang dibutuhkan terisi.
     */
    fun calculate(
        table: NormTable,
        inputs: Map<ComponentId, Double?>,
        formula: FormulaConfig = FormulaConfig.ESTIMATE_AB,
    ): CalculationResult {
        inputs.keys.forEach { id ->
            requireNotNull(table.component(id)) { "Komponen $id tidak ada pada norma '${table.id}'." }
        }
        val scores = LinkedHashMap<ComponentId, ComponentScore>()
        val missing = mutableListOf<ComponentId>()
        for (c in table.components) {
            val raw = inputs[c.id]
            if (raw == null) missing += c.id else scores[c.id] = scoreComponent(table, c.id, raw, formula.tieBreak)
        }

        fun groupMean(group: ComponentGroup): BigDecimal? {
            val members = table.components.filter { it.group == group }
            if (members.isEmpty() || members.any { it.id !in scores }) return null
            val sum = members.sumOf { scores.getValue(it.id).score }
            return BigDecimal(sum).divide(BigDecimal(members.size), INTERNAL_SCALE, RoundingMode.HALF_UP)
        }

        val a = groupMean(ComponentGroup.A)
        val b = groupMean(ComponentGroup.B)
        val c = groupMean(ComponentGroup.C)

        val wA = BigDecimal.valueOf(formula.weightA)
        val wB = BigDecimal.valueOf(formula.weightB)
        val combined = if (a != null && b != null) {
            (a * wA + b * wB).divide(wA + wB, formula.decimals, RoundingMode.HALF_UP)
        } else {
            null
        }
        val wC = formula.weightC?.let { BigDecimal.valueOf(it) }
        val finalWithC = if (a != null && b != null && c != null && wC != null) {
            (a * wA + b * wB + c * wC).divide(wA + wB + wC, formula.decimals, RoundingMode.HALF_UP)
        } else {
            null
        }

        val reasons = buildList {
            if (table.metadata.applicability != Applicability.CONFIRMED) {
                add("Norma belum dikonfirmasi berlaku untuk jenis seleksi dan periode ini.")
            }
            if (!formula.confirmed) {
                add("Rumus nilai gabungan belum dikonfirmasi resmi: ${formula.name}.")
            }
            val nonVerified = scores.values.count { it.matchedStatus != RowStatus.VERIFIED }
            if (nonVerified > 0) {
                add("$nonVerified komponen memakai baris estimasi linier atau berada di luar batas tabel.")
            }
        }

        return CalculationResult(
            components = scores,
            missing = missing,
            scoreA = a?.setScale(formula.decimals, RoundingMode.HALF_UP)?.toDouble(),
            averageB = b?.setScale(formula.decimals, RoundingMode.HALF_UP)?.toDouble(),
            scoreC = c?.setScale(formula.decimals, RoundingMode.HALF_UP)?.toDouble(),
            combinedAB = combined?.toDouble(),
            finalWithC = finalWithC?.toDouble(),
            label = if (reasons.isEmpty()) ResultLabel.OFFICIAL else ResultLabel.ESTIMATE,
            labelReasons = reasons,
            formulaId = formula.id,
        )
    }

    private fun meets(direction: Direction, raw: Double, threshold: Double): Boolean = when (direction) {
        Direction.HIGHER_IS_BETTER -> raw >= threshold
        Direction.LOWER_IS_BETTER -> raw <= threshold
    }

    private const val INTERNAL_SCALE = 10
}

/** Target latihan. Bukan prediksi nilai resmi: hanya membaca ambang dari norma aktif. */
object TargetPlanner {

    /** Ambang hasil agar mencapai [targetScore] menurut norma; null bila nilai itu tidak ada di tabel. */
    fun thresholdFor(table: NormTable, id: ComponentId, targetScore: Int): Double? =
        table.rows.firstOrNull { it.score == targetScore }?.thresholds?.get(id)

    /**
     * Selisih hasil sekarang ke target. Positif = masih kurang sebesar itu (dalam satuan komponen),
     * nol atau negatif = target sudah tercapai atau terlampaui. Mengikuti arah komponen.
     */
    fun gapToTarget(table: NormTable, id: ComponentId, raw: Double, targetScore: Int): Double? {
        val comp = table.component(id) ?: return null
        val threshold = thresholdFor(table, id, targetScore) ?: return null
        val gap = when (comp.direction) {
            Direction.HIGHER_IS_BETTER -> threshold - raw
            Direction.LOWER_IS_BETTER -> raw - threshold
        }
        return BigDecimal.valueOf(gap).setScale(4, RoundingMode.HALF_UP).toDouble()
    }
}
