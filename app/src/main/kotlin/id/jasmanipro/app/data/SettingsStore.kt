package id.jasmanipro.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.jasmanipro.domain.calc.TieBreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val tieBreak: TieBreak = TieBreak.HIGHEST_SCORE,
    val trackLengthMeters: Int = 400,
)

/** Penyimpanan preferensi di perangkat (DataStore). Tidak ada data yang keluar dari HP. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val tieBreak = stringPreferencesKey("tie_break")
        val track = intPreferencesKey("track_length_m")
    }

    val flow: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.theme]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            tieBreak = p[Keys.tieBreak]
                ?.let { runCatching { TieBreak.valueOf(it) }.getOrNull() }
                ?: TieBreak.HIGHEST_SCORE,
            trackLengthMeters = (p[Keys.track] ?: 400).takeIf { it in 100..1000 } ?: 400,
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setTieBreak(rule: TieBreak) {
        context.settingsDataStore.edit { it[Keys.tieBreak] = rule.name }
    }

    suspend fun setTrackLength(meters: Int) {
        context.settingsDataStore.edit { it[Keys.track] = meters }
    }
}
