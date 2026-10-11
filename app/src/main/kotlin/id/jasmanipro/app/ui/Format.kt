package id.jasmanipro.app.ui

import id.jasmanipro.domain.model.ComponentId
import java.math.BigDecimal
import java.math.RoundingMode

/** Angka berformat Indonesia: koma desimal, tanpa nol di belakang. 3444.0 -> "3444", 16.3 -> "16,3". */
fun fmt(value: Double, maxDecimals: Int = 2): String =
    BigDecimal.valueOf(value)
        .setScale(maxDecimals, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', ',')

fun ComponentId.label(): String = when (this) {
    ComponentId.LARI_12M -> "Lari 12 menit"
    ComponentId.PULL_UP -> "Pull-up"
    ComponentId.CHINNING -> "Chinning"
    ComponentId.SIT_UP -> "Sit-up"
    ComponentId.PUSH_UP -> "Push-up"
    ComponentId.SHUTTLE_RUN -> "Shuttle run"
    ComponentId.RENANG -> "Renang 25 m"
}
