package id.jasmanipro.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.jasmanipro.app.data.AppSettings
import id.jasmanipro.app.data.SettingsStore
import id.jasmanipro.app.data.ThemeMode
import id.jasmanipro.domain.calc.FormEvaluation
import id.jasmanipro.domain.calc.FormEvaluator
import id.jasmanipro.domain.calc.FormInput
import id.jasmanipro.domain.calc.FormulaConfig
import id.jasmanipro.domain.calc.LariMode
import id.jasmanipro.domain.calc.TieBreak
import id.jasmanipro.domain.model.ComponentId
import id.jasmanipro.domain.model.Gender
import id.jasmanipro.domain.model.NormTable
import id.jasmanipro.domain.norm.NormBundle
import id.jasmanipro.domain.norm.NormRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Menyimpan state layar Hitung (bertahan saat layar diputar) dan pengaturan.
 * Versi 0.1 belum menyimpan data peserta atau riwayat ke database.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore(application)
    private val loaded: Result<NormBundle> = runCatching { NormRepository.loadDefault() }

    val bundle: NormBundle? get() = loaded.getOrNull()
    val loadError: String? get() = loaded.exceptionOrNull()?.message

    val settings: StateFlow<AppSettings> =
        store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { store.setTheme(mode) }
    }

    fun setTieBreak(rule: TieBreak) {
        viewModelScope.launch { store.setTieBreak(rule) }
    }

    fun setTrackLength(meters: Int) {
        viewModelScope.launch { store.setTrackLength(meters) }
    }

    // ---- state form Hitung ----
    var gender by mutableStateOf(Gender.PRIA)
    val texts = mutableStateMapOf<ComponentId, String>()
    var lariMode by mutableStateOf(LariMode.METER)
    var lapsText by mutableStateOf("")
    var remainderText by mutableStateOf("")
    var targetScore by mutableStateOf(70)

    val table: NormTable? get() = bundle?.tableFor(gender)

    fun evaluate(settings: AppSettings): FormEvaluation? {
        val t = table ?: return null
        val input = FormInput(
            texts = texts.toMap(),
            lariMode = lariMode,
            lapsText = lapsText,
            remainderText = remainderText,
            trackLengthMeters = settings.trackLengthMeters,
        )
        val formula = FormulaConfig.ESTIMATE_AB.copy(tieBreak = settings.tieBreak)
        return FormEvaluator.evaluate(t, input, formula)
    }

    fun clearForm() {
        texts.clear()
        lapsText = ""
        remainderText = ""
    }
}
