package id.jasmanipro.domain

import id.jasmanipro.domain.calc.FormulaConfig
import id.jasmanipro.domain.calc.NormCalculator
import id.jasmanipro.domain.calc.RangeFlag
import id.jasmanipro.domain.calc.ResultLabel
import id.jasmanipro.domain.calc.TargetPlanner
import id.jasmanipro.domain.calc.TieBreak
import id.jasmanipro.domain.model.Applicability
import id.jasmanipro.domain.model.ComponentId
import id.jasmanipro.domain.model.Gender
import id.jasmanipro.domain.model.NormTable
import id.jasmanipro.domain.model.RowStatus
import id.jasmanipro.domain.norm.NormFormatException
import id.jasmanipro.domain.norm.NormRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Satu baris vektor uji emas yang dibuat tools/reference_check.py langsung dari file Excel. */
@Serializable
private data class Golden(
    val gender: String,
    val component: String,
    val input: Double,
    val scoreHighest: Int,
    val statusHighest: String? = null,
    val scoreLowest: Int,
    val statusLowest: String? = null,
    val rangeMin: Int? = null,
    val rangeMax: Int? = null,
    val flag: String,
)

class NormCalculatorTest {

    private val bundle = NormRepository.loadDefault()
    private val pria: NormTable = assertNotNull(bundle.tableFor(Gender.PRIA))
    private val wanita: NormTable = assertNotNull(bundle.tableFor(Gender.WANITA))

    private fun priaInputs(
        lari: Double?, pull: Double?, sit: Double?, push: Double?, shuttle: Double?, renang: Double?,
    ): Map<ComponentId, Double?> = mapOf(
        ComponentId.LARI_12M to lari,
        ComponentId.PULL_UP to pull,
        ComponentId.SIT_UP to sit,
        ComponentId.PUSH_UP to push,
        ComponentId.SHUTTLE_RUN to shuttle,
        ComponentId.RENANG to renang,
    )

    // ---------- data norma ----------

    @Test
    fun `norma termuat lengkap dan status per baris dipertahankan`() {
        assertEquals(2, bundle.tables.size)
        assertEquals(101, pria.rows.size)
        assertEquals(101, wanita.rows.size)
        assertEquals(listOf(100, 41, 0), pria.rows.filter { it.status == RowStatus.VERIFIED }.map { it.score })
        assertEquals(listOf(100), wanita.rows.filter { it.status == RowStatus.VERIFIED }.map { it.score })
        assertEquals(98, pria.rows.count { it.status == RowStatus.LINEAR_ESTIMATE })
        assertEquals(100, wanita.rows.count { it.status == RowStatus.LINEAR_ESTIMATE })
        assertEquals(Applicability.UNCONFIRMED, pria.metadata.applicability)
        assertEquals(Applicability.UNCONFIRMED, wanita.metadata.applicability)
        assertEquals(64, bundle.sourceSha256.length)
        assertTrue(pria.rows.filter { it.status == RowStatus.VERIFIED }.all { it.reference == "Kep/1352/VI/2020" })
        assertNotNull(wanita.component(ComponentId.CHINNING))
        assertNull(wanita.component(ComponentId.PULL_UP))
        assertNull(pria.component(ComponentId.CHINNING))
    }

    @Test
    fun `seluruh vektor emas cocok untuk kedua aturan tie break`() {
        val text = NormCalculatorTest::class.java.getResourceAsStream("/golden_vectors.json")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val vectors = Json.decodeFromString(ListSerializer(Golden.serializer()), text)
        assertTrue(vectors.size > 2000, "Vektor emas terlalu sedikit: ${vectors.size}")
        for (g in vectors) {
            val table = if (g.gender == "PRIA") pria else wanita
            val id = ComponentId.valueOf(g.component)
            val tag = "${g.gender} ${g.component} input=${g.input}"

            val hi = NormCalculator.scoreComponent(table, id, g.input, TieBreak.HIGHEST_SCORE)
            assertEquals(g.scoreHighest, hi.score, "nilai tertinggi: $tag")
            assertEquals(g.statusHighest, hi.matchedStatus?.name, "status tertinggi: $tag")
            assertEquals(g.rangeMin, hi.tiedScoreRange?.first, "rentang awal: $tag")
            assertEquals(g.rangeMax, hi.tiedScoreRange?.last, "rentang akhir: $tag")
            assertEquals(g.flag, hi.flag.name, "flag: $tag")

            val lo = NormCalculator.scoreComponent(table, id, g.input, TieBreak.LOWEST_SCORE)
            assertEquals(g.scoreLowest, lo.score, "nilai terendah: $tag")
            assertEquals(g.statusLowest, lo.matchedStatus?.name, "status terendah: $tag")
        }
    }

    // ---------- batas dan ambang ----------

    @Test
    fun `batas lari pria atas dan bawah`() {
        val top = NormCalculator.scoreComponent(pria, ComponentId.LARI_12M, 3444.0)
        assertEquals(100, top.score)
        assertEquals(RowStatus.VERIFIED, top.matchedStatus)
        assertEquals(RangeFlag.NONE, top.flag)

        assertEquals(RangeFlag.ABOVE_TABLE_MAX, NormCalculator.scoreComponent(pria, ComponentId.LARI_12M, 3445.0).flag)
        assertEquals(100, NormCalculator.scoreComponent(pria, ComponentId.LARI_12M, 5000.0).score)

        val floor = NormCalculator.scoreComponent(pria, ComponentId.LARI_12M, 1327.0)
        assertEquals(0, floor.score)
        assertEquals(RangeFlag.NONE, floor.flag)

        val below = NormCalculator.scoreComponent(pria, ComponentId.LARI_12M, 1326.0)
        assertEquals(0, below.score)
        assertEquals(RangeFlag.BELOW_TABLE_MIN, below.flag)
        assertNull(below.matchedStatus)
    }

    @Test
    fun `tepat di bawah dan di atas ambang nilai 70 pria`() {
        // Ambang nilai 70: lari 2809, pull-up 12, sit-up 28, push-up 31, shuttle 17.9, renang 35.
        // Nilai harapan dihitung oleh tools/reference_check.py dari file Excel.
        fun skor(id: ComponentId, raw: Double) = NormCalculator.scoreComponent(pria, id, raw).score

        assertEquals(69, skor(ComponentId.LARI_12M, 2808.0))
        assertEquals(70, skor(ComponentId.LARI_12M, 2809.0))
        assertEquals(70, skor(ComponentId.LARI_12M, 2810.0))

        assertEquals(64, skor(ComponentId.PULL_UP, 11.0))
        assertEquals(70, skor(ComponentId.PULL_UP, 12.0))
        assertEquals(76, skor(ComponentId.PULL_UP, 13.0))

        assertEquals(67, skor(ComponentId.SIT_UP, 27.0))
        assertEquals(70, skor(ComponentId.SIT_UP, 28.0))
        assertEquals(72, skor(ComponentId.SIT_UP, 29.0))

        assertEquals(67, skor(ComponentId.PUSH_UP, 30.0))
        assertEquals(70, skor(ComponentId.PUSH_UP, 31.0))
        assertEquals(73, skor(ComponentId.PUSH_UP, 32.0))

        assertEquals(70, skor(ComponentId.SHUTTLE_RUN, 17.9))
        assertEquals(68, skor(ComponentId.SHUTTLE_RUN, 17.91))

        assertEquals(70, skor(ComponentId.RENANG, 35.0))
        assertEquals(69, skor(ComponentId.RENANG, 35.05))
    }

    @Test
    fun `ambang kembar ditandai ambigu dan mengikuti aturan tie break`() {
        val hi = NormCalculator.scoreComponent(pria, ComponentId.PULL_UP, 17.0, TieBreak.HIGHEST_SCORE)
        assertEquals(100, hi.score)
        assertEquals(95..100, hi.tiedScoreRange)
        val lo = NormCalculator.scoreComponent(pria, ComponentId.PULL_UP, 17.0, TieBreak.LOWEST_SCORE)
        assertEquals(95, lo.score)
        assertEquals(RowStatus.LINEAR_ESTIMATE, lo.matchedStatus)
    }

    @Test
    fun `waktu desimal shuttle run pria`() {
        assertEquals(100, NormCalculator.scoreComponent(pria, ComponentId.SHUTTLE_RUN, 16.2).score)
        val sedikitLambat = NormCalculator.scoreComponent(pria, ComponentId.SHUTTLE_RUN, 16.21)
        assertEquals(99, sedikitLambat.score)
        assertEquals(98..99, sedikitLambat.tiedScoreRange)
    }

    @Test
    fun `wanita push-up nol cocok dengan banyak baris dan bergantung aturan tie break`() {
        val hi = NormCalculator.scoreComponent(wanita, ComponentId.PUSH_UP, 0.0, TieBreak.HIGHEST_SCORE)
        assertEquals(38, hi.score)
        assertEquals(0..38, hi.tiedScoreRange)
        val lo = NormCalculator.scoreComponent(wanita, ComponentId.PUSH_UP, 0.0, TieBreak.LOWEST_SCORE)
        assertEquals(0, lo.score)
    }

    @Test
    fun `input nol bukan input kosong`() {
        val nolPullUp = NormCalculator.scoreComponent(pria, ComponentId.PULL_UP, 0.0)
        assertEquals(0, nolPullUp.score)
        assertEquals(RangeFlag.BELOW_TABLE_MIN, nolPullUp.flag)

        val hasil = NormCalculator.calculate(pria, priaInputs(2809.0, null, 28.0, 31.0, 17.9, 35.0))
        assertEquals(listOf(ComponentId.PULL_UP), hasil.missing)
        assertTrue(ComponentId.PULL_UP !in hasil.components)
    }

    @Test
    fun `input negatif dan tidak terhingga ditolak`() {
        assertFailsWith<IllegalArgumentException> { NormCalculator.scoreComponent(pria, ComponentId.LARI_12M, -1.0) }
        assertFailsWith<IllegalArgumentException> { NormCalculator.scoreComponent(pria, ComponentId.LARI_12M, Double.NaN) }
        assertFailsWith<IllegalArgumentException> {
            NormCalculator.scoreComponent(pria, ComponentId.LARI_12M, Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `komponen yang tidak ada pada norma ditolak`() {
        assertFailsWith<IllegalArgumentException> { NormCalculator.scoreComponent(wanita, ComponentId.PULL_UP, 5.0) }
        assertFailsWith<IllegalArgumentException> { NormCalculator.scoreComponent(pria, ComponentId.CHINNING, 5.0) }
        assertFailsWith<IllegalArgumentException> {
            NormCalculator.calculate(wanita, mapOf(ComponentId.PULL_UP to 5.0))
        }
    }

    // ---------- rata-rata B, gabungan, pembulatan ----------

    @Test
    fun `semua komponen di ambang nilai 70 menghasilkan 70`() {
        val r = NormCalculator.calculate(pria, priaInputs(2809.0, 12.0, 28.0, 31.0, 17.9, 35.0))
        assertTrue(r.isComplete)
        assertEquals(70.0, r.scoreA)
        assertEquals(70.0, r.averageB)
        assertEquals(70.0, r.scoreC)
        assertEquals(70.0, r.combinedAB)
        assertNull(r.finalWithC)
    }

    @Test
    fun `rata-rata B dan gabungan dibulatkan setengah ke atas dua desimal`() {
        // nilai: lari 79, pull-up 76, sit-up 77, push-up 75, shuttle 77, renang 77
        val r = NormCalculator.calculate(pria, priaInputs(3000.0, 13.0, 31.0, 33.0, 17.45, 30.0))
        assertEquals(79, r.components.getValue(ComponentId.LARI_12M).score)
        assertEquals(76, r.components.getValue(ComponentId.PULL_UP).score)
        assertEquals(77, r.components.getValue(ComponentId.SIT_UP).score)
        assertEquals(75, r.components.getValue(ComponentId.PUSH_UP).score)
        assertEquals(77, r.components.getValue(ComponentId.SHUTTLE_RUN).score)
        assertEquals(77, r.components.getValue(ComponentId.RENANG).score)
        assertEquals(76.25, r.averageB)
        assertEquals(77.63, r.combinedAB) // 77,625 dibulatkan setengah ke atas
    }

    @Test
    fun `nilai akhir dengan C hanya dihitung bila bobot C diberikan`() {
        val input = priaInputs(3000.0, 13.0, 31.0, 33.0, 17.45, 30.0)
        assertNull(NormCalculator.calculate(pria, input).finalWithC)
        val withC = FormulaConfig.ESTIMATE_AB.copy(weightC = 1.0)
        assertEquals(77.42, NormCalculator.calculate(pria, input, withC).finalWithC) // (79 + 76,25 + 77) / 3
        val tanpaC = input + (ComponentId.RENANG to null)
        assertNull(NormCalculator.calculate(pria, tanpaC, withC).finalWithC)
    }

    @Test
    fun `data tidak lengkap tidak menghasilkan rata-rata atau gabungan`() {
        val r = NormCalculator.calculate(pria, priaInputs(2809.0, 12.0, 28.0, 31.0, null, 35.0))
        assertEquals(listOf(ComponentId.SHUTTLE_RUN), r.missing)
        assertEquals(false, r.isComplete)
        assertEquals(70.0, r.scoreA)
        assertNull(r.averageB)
        assertNull(r.combinedAB)
        assertNull(r.finalWithC)
    }

    @Test
    fun `norma wanita memakai chinning pada kelompok B`() {
        val inputs = mapOf(
            ComponentId.LARI_12M to 2000.0,
            ComponentId.CHINNING to 30.0,
            ComponentId.SIT_UP to 20.0,
            ComponentId.PUSH_UP to 10.0,
            ComponentId.SHUTTLE_RUN to 20.0,
            ComponentId.RENANG to 50.0,
        )
        val r = NormCalculator.calculate(wanita, inputs)
        assertTrue(r.isComplete)
        assertNotNull(r.averageB)
        assertNotNull(r.combinedAB)
    }

    @Test
    fun `aturan tie break dari konfigurasi dipakai oleh calculate`() {
        val inputs = mapOf(ComponentId.PUSH_UP to 0.0)
        val tinggi = NormCalculator.calculate(wanita, inputs)
        val rendah = NormCalculator.calculate(wanita, inputs, FormulaConfig.ESTIMATE_AB.copy(tieBreak = TieBreak.LOWEST_SCORE))
        assertEquals(38, tinggi.components.getValue(ComponentId.PUSH_UP).score)
        assertEquals(0, rendah.components.getValue(ComponentId.PUSH_UP).score)
    }

    // ---------- label resmi / estimasi ----------

    @Test
    fun `norma bawaan tidak pernah berlabel resmi walau semua hasil 100`() {
        val r = NormCalculator.calculate(pria, priaInputs(3444.0, 17.0, 40.0, 42.0, 16.2, 14.0))
        assertTrue(r.components.values.all { it.score == 100 && it.matchedStatus == RowStatus.VERIFIED })
        assertEquals(ResultLabel.ESTIMATE, r.label)
        assertTrue(r.labelReasons.any { it.contains("belum dikonfirmasi") })
    }

    @Test
    fun `label resmi hanya bila norma rumus dan seluruh baris terverifikasi`() {
        val dikonfirmasi = pria.copy(metadata = pria.metadata.copy(applicability = Applicability.CONFIRMED))
        val rumus = FormulaConfig.ESTIMATE_AB.copy(confirmed = true)

        val semua100 = priaInputs(3444.0, 17.0, 40.0, 42.0, 16.2, 14.0)
        assertEquals(ResultLabel.OFFICIAL, NormCalculator.calculate(dikonfirmasi, semua100, rumus).label)
        // rumus belum dikonfirmasi -> tetap estimasi
        assertEquals(ResultLabel.ESTIMATE, NormCalculator.calculate(dikonfirmasi, semua100).label)
        // baris estimasi linier terpakai -> tetap estimasi
        val nilai70 = priaInputs(2809.0, 12.0, 28.0, 31.0, 17.9, 35.0)
        assertEquals(ResultLabel.ESTIMATE, NormCalculator.calculate(dikonfirmasi, nilai70, rumus).label)
    }

    // ---------- validasi norma ----------

    @Test
    fun `tabel norma yang rusak ditolak`() {
        assertFailsWith<IllegalArgumentException> { pria.copy(rows = pria.rows.reversed()) }
        assertFailsWith<IllegalArgumentException> { pria.copy(rows = emptyList()) }
        val rusak = pria.rows.toMutableList()
        rusak[10] = rusak[10].copy(thresholds = rusak[10].thresholds + (ComponentId.LARI_12M to 9999.0))
        assertFailsWith<IllegalArgumentException> { pria.copy(rows = rusak) }
    }

    @Test
    fun `berkas norma tidak valid ditolak dengan pesan jelas`() {
        assertFailsWith<NormFormatException> { NormRepository.parse("bukan json") }
        val skemaBaru = """{"schemaVersion":2,"source":{"fileName":"x","sha256":"y"},"tables":[]}"""
        val e = assertFailsWith<NormFormatException> { NormRepository.parse(skemaBaru) }
        assertTrue(e.message!!.contains("tidak didukung"))
    }

    // ---------- target latihan ----------

    @Test
    fun `selisih menuju target mengikuti arah komponen`() {
        assertEquals(2809.0, TargetPlanner.thresholdFor(pria, ComponentId.LARI_12M, 70))
        assertEquals(109.0, TargetPlanner.gapToTarget(pria, ComponentId.LARI_12M, 2700.0, 70)) // kurang 109 m
        assertEquals(-191.0, TargetPlanner.gapToTarget(pria, ComponentId.LARI_12M, 3000.0, 70)) // lebih 191 m
        assertEquals(0.5, TargetPlanner.gapToTarget(pria, ComponentId.SHUTTLE_RUN, 18.4, 70)) // harus 0,5 detik lebih cepat
        assertNull(TargetPlanner.thresholdFor(pria, ComponentId.LARI_12M, 101))
        assertNull(TargetPlanner.gapToTarget(wanita, ComponentId.PULL_UP, 5.0, 70))
    }
}
