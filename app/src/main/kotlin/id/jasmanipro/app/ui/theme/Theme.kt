package id.jasmanipro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet identitas Jasmani Pro.
val Navy = Color(0xFF0B1736)
val BluePrimary = Color(0xFF2563EB)
val Cyan = Color(0xFF06B6D4)
val StatusGreen = Color(0xFF16A34A)
val StatusYellow = Color(0xFFFACC15)
val StatusRed = Color(0xFFDC2626)

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE6FF),
    onPrimaryContainer = Navy,
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E4FF),
    onSecondaryContainer = Navy,
    tertiary = StatusGreen,
    onTertiary = Color.White,
    error = StatusRed,
    onError = Color.White,
    background = Color(0xFFF5F7FB),
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFE8EDF7),
    onSurfaceVariant = Color(0xFF4A5673),
    outline = Color(0xFF8A94AD),
    outlineVariant = Color(0xFFDCE3F0),
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFEEF2FA),
    surfaceContainerHighest = Color(0xFFE8EDF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6B9BFF),
    onPrimary = Navy,
    primaryContainer = Color(0xFF1B3A7A),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFF22D3EE),
    onSecondary = Navy,
    secondaryContainer = Color(0xFF1D3A66),
    onSecondaryContainer = Color(0xFFDCE6FF),
    tertiary = Color(0xFF22C55E),
    onTertiary = Navy,
    error = Color(0xFFF87171),
    onError = Navy,
    background = Color(0xFF101827),
    onBackground = Color(0xFFE6ECF8),
    surface = Color(0xFF162033),
    onSurface = Color(0xFFE6ECF8),
    surfaceVariant = Color(0xFF1E2A44),
    onSurfaceVariant = Color(0xFFA9B5CF),
    outline = Color(0xFF6C7893),
    outlineVariant = Color(0xFF2B3752),
    surfaceContainer = Color(0xFF162033),
    surfaceContainerLow = Color(0xFF131C2E),
    surfaceContainerHigh = Color(0xFF1B2740),
    surfaceContainerHighest = Color(0xFF1E2A44),
)

@Composable
fun JasmaniTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
