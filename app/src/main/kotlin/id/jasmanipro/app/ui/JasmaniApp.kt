package id.jasmanipro.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import id.jasmanipro.app.AppViewModel
import id.jasmanipro.app.data.ThemeMode
import id.jasmanipro.app.ui.calculator.CalculatorScreen
import id.jasmanipro.app.ui.home.HomeScreen
import id.jasmanipro.app.ui.norms.NormScreen
import id.jasmanipro.app.ui.settings.SettingsScreen
import id.jasmanipro.app.ui.theme.JasmaniTheme

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Beranda", Icons.Filled.Home),
    TabItem("Hitung", Icons.Filled.Edit),
    TabItem("Norma", Icons.Filled.Info),
    TabItem("Pengaturan", Icons.Filled.Settings),
)

@Composable
fun JasmaniApp(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    JasmaniTheme(darkTheme = dark) {
        var tab by rememberSaveable { mutableStateOf(0) }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = { tab = index },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                Box(modifier = Modifier.padding(padding).statusBarsPadding().fillMaxSize()) {
                    when (tab) {
                        0 -> HomeScreen(vm, onStart = { tab = 1 })
                        1 -> CalculatorScreen(vm, settings)
                        2 -> NormScreen(vm)
                        else -> SettingsScreen(vm, settings)
                    }
                }
            }
        }
    }
}
