package id.jasmanipro.domain

import id.jasmanipro.domain.calc.FormEvaluator
import id.jasmanipro.domain.calc.FormInput
import id.jasmanipro.domain.calc.LariMode
import id.jasmanipro.domain.input.ParseResult
import id.jasmanipro.domain.model.ComponentId
import id.jasmanipro.domain.model.Gender
import id.jasmanipro.domain.model.NormTable
import id.jasmanipro.domain.model.RowStatus
import id.jasmanipro.domain.norm.NormRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FormEvaluatorTest {

    private val bundle = NormRepository.loadDefault()
    private val pria: NormTable = assertNotNull(bundle.tableFor(Gender.PRIA))
    private val wanita: NormTable = assertNotNull(bundle.tableFor(Gender.WANITA))

    @Test
    fun `form kosong tidak menghasilkan nilai dan tidak dianggap tidak valid`() {
        val e = FormEvaluator.evaluate(pria, FormInput())
        assertTrue(e.fields.values.all { it == ParseResult.Missing })
        assertFalse(e.hasInvalid)
        assertTrue(e.result.components.isEmpty())
        assertEquals(6, e.result.missing.size)
    }

    @Test
    fun `mode meter memberi nilai lari`() {
        val e = FormEvaluator.evaluate(pria, FormInput(texts = mapOf(ComponentId.LARI_12M to "2809")))
        assertEquals(70, e.result.components.getValue(ComponentId.LARI_12M).score)
        assertEquals(2809.0, e.lariMeters)
    }

    @Test
    fun `mode kilometer dikonversi ke meter`() {
        val input = FormInput(texts = mapOf(ComponentId.LARI_12M to "3,444"), lariMode = LariMode.KILOMETER)
        val e = FormEvaluator.evaluate(pria, input)
        assertEquals(3444.0, e.lariMeters)
        val s = e.result.components.getValue(ComponentId.LARI_12M)
        assertEquals(100, s.score)
        assertEquals(RowStatus.VERIFIED, s.matchedStatus)
    }

    @Test
    fun `mode putaran menghitung total meter dan menolak sisa yang tidak konsisten`() {
        val ok = FormEvaluator.evaluate(
            pria,
            FormInput(lariMode = LariMode.LAPS, lapsText = "8", remainderText = "244"),
        )
        assertEquals(3444.0, ok.lariMeters)

        val salah = FormEvaluator.evaluate(
            pria,
            FormInput(lariMode = LariMode.LAPS, lapsText = "8", remainderText = "400"),
        )
        assertIs<ParseResult.Invalid>(salah.fields.getValue(ComponentId.LARI_12M))
        assertTrue(salah.hasInvalid)
        assertTrue(ComponentId.LARI_12M !in salah.result.components)

        val kosong = FormEvaluator.evaluate(pria, FormInput(lariMode = LariMode.LAPS))
        assertEquals(ParseResult.Missing, kosong.fields.getValue(ComponentId.LARI_12M))
    }

    @Test
    fun `isian tidak valid diperlakukan sebagai belum diisi bukan nol`() {
        val e = FormEvaluator.evaluate(pria, FormInput(texts = mapOf(ComponentId.PULL_UP to "abc")))
        assertIs<ParseResult.Invalid>(e.fields.getValue(ComponentId.PULL_UP))
        assertTrue(e.hasInvalid)
        assertTrue(ComponentId.PULL_UP in e.result.missing)
        assertTrue(ComponentId.PULL_UP !in e.result.components)
    }

    @Test
    fun `semua isian di ambang nilai 70 memberi gabungan 70 dengan koma desimal`() {
        val texts = mapOf(
            ComponentId.LARI_12M to "2809",
            ComponentId.PULL_UP to "12",
            ComponentId.SIT_UP to "28",
            ComponentId.PUSH_UP to "31",
            ComponentId.SHUTTLE_RUN to "17,9",
            ComponentId.RENANG to "35",
        )
        val e = FormEvaluator.evaluate(pria, FormInput(texts = texts))
        assertFalse(e.hasInvalid)
        assertTrue(e.result.isComplete)
        assertEquals(70.0, e.result.combinedAB)
    }

    @Test
    fun `norma wanita mengabaikan isian pull-up dan memakai chinning`() {
        val texts = mapOf(ComponentId.PULL_UP to "5", ComponentId.CHINNING to "30")
        val e = FormEvaluator.evaluate(wanita, FormInput(texts = texts))
        assertTrue(ComponentId.PULL_UP !in e.fields)
        assertTrue(ComponentId.CHINNING in e.result.components)
    }
}
