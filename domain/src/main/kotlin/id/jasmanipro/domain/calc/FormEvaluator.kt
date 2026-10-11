package id.jasmanipro.domain.calc

import id.jasmanipro.domain.input.InputValidator
import id.jasmanipro.domain.input.ParseResult
import id.jasmanipro.domain.model.ComponentId
import id.jasmanipro.domain.model.NormTable

/** Cara pengguna memasukkan jarak lari 12 menit. */
enum class LariMode { METER, KILOMETER, LAPS }

/** Seluruh isian layar hitung dalam bentuk teks mentah, persis seperti diketik pengguna. */
data class FormInput(
    val texts: Map<ComponentId, String> = emptyMap(),
    val lariMode: LariMode = LariMode.METER,
    val lapsText: String = "",
    val remainderText: String = "",
    val trackLengthMeters: Int = 400,
)

data class FormEvaluation(
    /** Hasil validasi tiap komponen yang ada pada norma aktif. */
    val fields: Map<ComponentId, ParseResult>,
    /** Jarak lari dalam meter bila valid, apa pun mode inputnya. */
    val lariMeters: Double?,
    val result: CalculationResult,
    val hasInvalid: Boolean,
)

/**
 * Menghubungkan validasi input dengan kalkulator. Isian yang kosong ATAU tidak valid
 * diperlakukan sebagai belum diisi (tidak pernah menjadi nol).
 */
object FormEvaluator {

    fun evaluate(
        table: NormTable,
        input: FormInput,
        formula: FormulaConfig = FormulaConfig.ESTIMATE_AB,
    ): FormEvaluation {
        val parsed = LinkedHashMap<ComponentId, ParseResult>()
        for (c in table.components) {
            parsed[c.id] = if (c.id == ComponentId.LARI_12M) {
                parseLari(input)
            } else {
                InputValidator.validate(c.id, input.texts[c.id])
            }
        }
        val values: Map<ComponentId, Double?> = parsed.mapValues { (it.value as? ParseResult.Valid)?.value }
        val result = NormCalculator.calculate(table, values, formula)
        val lari = (parsed[ComponentId.LARI_12M] as? ParseResult.Valid)?.value
        return FormEvaluation(
            fields = parsed,
            lariMeters = lari,
            result = result,
            hasInvalid = parsed.values.any { it is ParseResult.Invalid },
        )
    }

    private fun parseLari(input: FormInput): ParseResult = when (input.lariMode) {
        LariMode.METER -> InputValidator.validate(ComponentId.LARI_12M, input.texts[ComponentId.LARI_12M])
        LariMode.KILOMETER -> InputValidator.kilometersToMeters(input.texts[ComponentId.LARI_12M])
        LariMode.LAPS ->
            if (input.lapsText.isBlank() && input.remainderText.isBlank()) {
                ParseResult.Missing
            } else {
                InputValidator.lapsToMeters(input.lapsText, input.remainderText, input.trackLengthMeters)
            }
    }
}
