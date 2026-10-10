package id.jasmanipro.domain

import id.jasmanipro.domain.input.InputValidator
import id.jasmanipro.domain.input.ParseResult
import id.jasmanipro.domain.model.ComponentId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InputValidatorTest {

    private fun valid(r: ParseResult): ParseResult.Valid = assertIs<ParseResult.Valid>(r)
    private fun invalid(r: ParseResult): ParseResult.Invalid = assertIs<ParseResult.Invalid>(r)

    @Test
    fun `koma dan titik sama-sama diterima sebagai desimal`() {
        assertEquals(ParseResult.Valid(16.3), InputValidator.validate(ComponentId.SHUTTLE_RUN, "16,3"))
        assertEquals(ParseResult.Valid(16.3), InputValidator.validate(ComponentId.SHUTTLE_RUN, "16.3"))
        assertEquals(ParseResult.Valid(16.3), InputValidator.validate(ComponentId.SHUTTLE_RUN, " 16,3 "))
    }

    @Test
    fun `kolom kosong adalah missing bukan nol`() {
        for (id in ComponentId.entries) {
            assertEquals(ParseResult.Missing, InputValidator.validate(id, null))
            assertEquals(ParseResult.Missing, InputValidator.validate(id, ""))
            assertEquals(ParseResult.Missing, InputValidator.validate(id, "   "))
        }
    }

    @Test
    fun `nol repetisi sah tetapi waktu nol ditolak`() {
        assertEquals(ParseResult.Valid(0.0), InputValidator.validate(ComponentId.PULL_UP, "0"))
        assertEquals(ParseResult.Valid(0.0), InputValidator.validate(ComponentId.CHINNING, "0"))
        assertTrue(invalid(InputValidator.validate(ComponentId.SHUTTLE_RUN, "0")).message.contains("lebih dari 0"))
        invalid(InputValidator.validate(ComponentId.RENANG, "0,0"))
    }

    @Test
    fun `nilai negatif ditolak untuk semua komponen`() {
        for (id in ComponentId.entries) {
            assertTrue(invalid(InputValidator.validate(id, "-1")).message.contains("negatif"), "komponen $id")
        }
    }

    @Test
    fun `repetisi harus bilangan bulat`() {
        assertTrue(invalid(InputValidator.validate(ComponentId.PUSH_UP, "12,5")).message.contains("bulat"))
        assertEquals(ParseResult.Valid(12.0), InputValidator.validate(ComponentId.PUSH_UP, "12"))
        assertEquals(ParseResult.Valid(12.0), InputValidator.validate(ComponentId.PUSH_UP, "12,0"))
    }

    @Test
    fun `pemisah ribuan dan teks bukan angka ditolak dengan petunjuk`() {
        assertTrue(invalid(InputValidator.validate(ComponentId.LARI_12M, "3.444,5")).message.contains("pemisah ribuan"))
        invalid(InputValidator.validate(ComponentId.LARI_12M, "abc"))
        invalid(InputValidator.validate(ComponentId.LARI_12M, "12a"))
        invalid(InputValidator.validate(ComponentId.LARI_12M, "1e3"))
    }

    @Test
    fun `nilai tidak wajar hanya memberi peringatan`() {
        val kecil = valid(InputValidator.validate(ComponentId.LARI_12M, "3.444"))
        assertEquals(3.444, kecil.value)
        assertTrue(kecil.warnings.any { it.contains("kilometer") })

        assertTrue(valid(InputValidator.validate(ComponentId.LARI_12M, "6000")).warnings.isNotEmpty())
        assertTrue(valid(InputValidator.validate(ComponentId.SHUTTLE_RUN, "5")).warnings.isNotEmpty())
        assertTrue(valid(InputValidator.validate(ComponentId.PULL_UP, "300")).warnings.isNotEmpty())
        assertTrue(valid(InputValidator.validate(ComponentId.LARI_12M, "3000")).warnings.isEmpty())
    }

    @Test
    fun `kilometer dikonversi tepat ke meter`() {
        assertEquals(ParseResult.Valid(3444.0), InputValidator.kilometersToMeters("3,444"))
        assertEquals(ParseResult.Valid(3444.0), InputValidator.kilometersToMeters("3.444"))
        assertEquals(ParseResult.Valid(3444.5), InputValidator.kilometersToMeters("3.4445"))
        assertEquals(ParseResult.Missing, InputValidator.kilometersToMeters(""))
        invalid(InputValidator.kilometersToMeters("-3"))
    }

    @Test
    fun `putaran dan sisa meter dikonversi sesuai panjang lintasan`() {
        assertEquals(ParseResult.Valid(3444.0), InputValidator.lapsToMeters("8", "244"))
        assertEquals(ParseResult.Valid(3350.0), InputValidator.lapsToMeters("13", "100", trackLengthMeters = 250))
        assertEquals(ParseResult.Valid(3200.0), InputValidator.lapsToMeters("8", "0"))
    }

    @Test
    fun `putaran yang tidak konsisten dengan jarak ditolak`() {
        assertTrue(invalid(InputValidator.lapsToMeters("8", "400")).message.contains("400"))
        invalid(InputValidator.lapsToMeters("8", "450"))
        invalid(InputValidator.lapsToMeters("8,5", "10"))
        invalid(InputValidator.lapsToMeters("", "10"))
        invalid(InputValidator.lapsToMeters("8", ""))
        invalid(InputValidator.lapsToMeters("8", "-5"))
        invalid(InputValidator.lapsToMeters("8", "10", trackLengthMeters = 0))
    }
}
