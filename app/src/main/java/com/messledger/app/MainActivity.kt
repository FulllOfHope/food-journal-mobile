package com.messledger.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModelProvider
import com.messledger.app.data.ConfigDataStore
import com.messledger.app.data.MessLedgerDatabase
import com.messledger.app.data.ThemeMode
import com.messledger.app.ui.MainScreen
import com.messledger.app.ui.theme.DarkMessColors
import com.messledger.app.updates.UpdateManager
import com.messledger.app.updates.UpdateNotifier
import com.messledger.app.ui.theme.FoodJournalTheme
import com.messledger.app.ui.theme.LightMessColors
import com.messledger.app.viewmodel.MessViewModel
import com.messledger.app.viewmodel.MessViewModelFactory
import com.messledger.app.widget.MessGlanceWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = MessLedgerDatabase.getDatabase(applicationContext)
        val configDataStore = ConfigDataStore(applicationContext)
        val updateManager = UpdateManager.get(applicationContext)
        if (intent?.action == UpdateNotifier.ACTION_SHOW_UPDATE) {
            updateManager.showSheetFromNotification()
        }

        // Resolve the saved preference before the first frame so the window
        // never flashes light while Compose catches up.
        val initialDark = runBlocking {
            when (configDataStore.themeModeFlow.first()) {
                ThemeMode.SYSTEM -> resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
        }
        val initialCanvas = if (initialDark) DarkMessColors.background else LightMessColors.background
        window.statusBarColor = initialCanvas.toArgb()
        window.navigationBarColor = initialCanvas.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !initialDark
            isAppearanceLightNavigationBars = !initialDark
        }

        val factory = MessViewModelFactory(db.messLedgerDao(), configDataStore)
        val viewModel = ViewModelProvider(this, factory)[MessViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val messState by viewModel.state.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep the home-screen widget honest: meal changes, guest additions,
            // price edits and theme flips all refresh it.
            LaunchedEffect(darkTheme, messState.cycleMeals, messState.config) {
                MessGlanceWidget().updateAll(applicationContext)
            }

            FoodJournalTheme(darkTheme = darkTheme) {
                MainScreen(
                    viewModel = viewModel,
                    isDark = darkTheme,
                    themeMode = themeMode,
                    updateManager = updateManager,
                    onToggleTheme = { viewModel.toggleTheme(darkTheme) },
                    onResetTheme = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // A content tap while the journal is already open arrives here, not
        // in onCreate, and must still raise the update sheet.
        if (intent.action == UpdateNotifier.ACTION_SHOW_UPDATE) {
            UpdateManager.get(applicationContext).showSheetFromNotification()
        }
    }
}
