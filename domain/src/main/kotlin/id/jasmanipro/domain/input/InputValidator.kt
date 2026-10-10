package id.jasmanipro.domain.input

import id.jasmanipro.domain.model.ComponentId
import java.math.BigDecimal

/** Hasil validasi satu kolom. Kolom kosong adalah [Missing], bukan nol. */
sealed interface ParseResult {
    data object Missing : ParseResult
    data class Valid(val value: Double, val warnings: List<String> = emptyList()) : ParseResult
    data class Invalid(val message: String) : ParseResult
}

object InputValidator {

    private enum class Kind { DISTANCE_METERS, WHOLE_COUNT, SECONDS }

    private val kinds = mapOf(
        ComponentId.LARI_12M to Kind.DISTANCE_METERS,
        ComponentId.PULL_UP to Kind.WHOLE_COUNT,
        ComponentId.CHINNING to Kind.WHOLE_COUNT,
        ComponentId.SIT_UP to Kind.WHOLE_COUNT,
        ComponentId.PUSH_UP to Kind.WHOLE_COUNT,
        ComponentId.SHUTTLE_RUN to Kind.SECONDS,
        ComponentId.RENANG to Kind.SECONDS,
    )

    private val numberPattern = Regex("""^\d+([.,]\d+)?$""")

    /** Batas kewajaran (hanya peringatan, bukan penolakan). */
    private const val LARI_MAX_WAJAR = 4500.0
    private const val LARI_MIN_WAJAR = 500.0
    private const val REPETISI_MAX_WAJAR = 200.0
    private const val WAKTU_MIN_WAJAR = 8.0

    fun validate(id: ComponentId, text: String?): ParseResult {
        val kind = kinds.getValue(id)
        val number = parseNumber(text)
        if (number !is ParseResult.Valid) return number
        val v = number.value
        val warnings = mutableListOf<String>()
        when (kind) {
            Kind.WHOLE_COUNT -> {
                if (v != Math.floor(v)) {
                    return ParseResult.Invalid("Jumlah repetisi harus bilangan bulat (contoh: 12), bukan pecahan.")
                }
                if (v > REPETISI_MAX_WAJAR) warnings += "Jumlah repetisi ${fmt(v)} tampak sangat tinggi. Periksa kembali."
            }
            Kind.SECONDS -> {
                if (v <= 0.0) return ParseResult.Invalid("Waktu harus lebih dari 0 detik.")
                if (v < WAKTU_MIN_WAJAR) warnings += "Waktu ${fmt(v)} detik tampak sangat cepat. Periksa kembali."
            }
            Kind.DISTANCE_METERS -> {
                if (v > LARI_MAX_WAJAR) warnings += "Jarak ${fmt(v)} m tampak sangat jauh untuk 12 menit. Periksa kembali."
                if (v < LARI_MIN_WAJAR) {
                    warnings += "Jarak ${fmt(v)} m tampak sangat pendek. Jika maksudnya kilometer, gunakan input kilometer."
                }
            }
        }
        return ParseResult.Valid(v, warnings)
    }

    /** Kilometer -> meter memakai BigDecimal agar 3,444 km tepat 3444 m (tanpa galat floating point). */
    fun kilometersToMeters(text: String?): ParseResult {
        val number = parseNumber(text)
        if (number !is ParseResult.Valid) return number
        val meters = BigDecimal(canonical(text!!)).multiply(BigDecimal(1000))
        return validate(ComponentId.LARI_12M, plain(meters))
    }

    /** Putaran lintasan + sisa meter -> total meter. Sisa harus kurang dari panjang lintasan. */
    fun lapsToMeters(lapsText: String?, remainderText: String?, trackLengthMeters: Int = 400): ParseResult {
        if (trackLengthMeters <= 0) return ParseResult.Invalid("Panjang lintasan harus lebih dari 0 meter.")
        val laps = parseNumber(lapsText)
        if (laps !is ParseResult.Valid) {
            return if (laps is ParseResult.Missing) ParseResult.Invalid("Isi jumlah putaran (boleh 0).") else laps
        }
        if (laps.value != Math.floor(laps.value)) {
            return ParseResult.Invalid("Jumlah putaran harus bilangan bulat (contoh: 8).")
        }
        val remainder = parseNumber(remainderText)
        val rem = when (remainder) {
            is ParseResult.Valid -> remainder.value
            is ParseResult.Missing -> return ParseResult.Invalid("Isi sisa meter (boleh 0).")
            is ParseResult.Invalid -> return remainder
        }
        if (rem >= trackLengthMeters) {
            return ParseResult.Invalid(
                "Sisa ${fmt(rem)} m harus kurang dari panjang lintasan $trackLengthMeters m. " +
                    "Tambahkan ke jumlah putaran.",
            )
        }
        val total = BigDecimal(laps.value.toLong()).multiply(BigDecimal(trackLengthMeters))
            .add(BigDecimal(canonical(remainderText!!)))
        return validate(ComponentId.LARI_12M, plain(total))
    }

    private fun parseNumber(text: String?): ParseResult {
        val t = text?.replace('\u00A0', ' ')?.trim().orEmpty()
        if (t.isEmpty()) return ParseResult.Missing
        if (t.startsWith("-")) return ParseResult.Invalid("Nilai tidak boleh negatif.")
        if (!numberPattern.matches(t)) {
            val separators = t.count { it == '.' || it == ',' }
            return ParseResult.Invalid(
                if (separators > 1) {
                    "Gunakan satu tanda desimal (koma atau titik) tanpa pemisah ribuan. Contoh: 16,3 atau 3444."
                } else {
                    "Masukkan angka saja, contoh: 16,3 atau 3444."
                },
            )
        }
        return ParseResult.Valid(canonical(t).toDouble())
    }

    private fun canonical(t: String): String = t.trim().replace(',', '.')

    /** Tanpa notasi ilmiah supaya hasilnya lolos kembali melalui parser angka. */
    private fun plain(v: BigDecimal): String = v.stripTrailingZeros().toPlainString()

    private fun fmt(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else v.toString()
}
