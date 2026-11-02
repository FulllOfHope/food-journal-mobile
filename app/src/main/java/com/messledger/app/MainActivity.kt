package com.messledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModelProvider
import com.messledger.app.data.ConfigDataStore
import com.messledger.app.data.MessLedgerDatabase
import com.messledger.app.data.ThemeMode
import com.messledger.app.ui.MainScreen
import com.messledger.app.ui.theme.FoodJournalTheme
import com.messledger.app.viewmodel.MessViewModel
import com.messledger.app.viewmodel.MessViewModelFactory
import com.messledger.app.widget.MessGlanceWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = MessLedgerDatabase.getDatabase(applicationContext)
        val configDataStore = ConfigDataStore(applicationContext)
        
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
                    onToggleTheme = { viewModel.toggleTheme(darkTheme) },
                    onResetTheme = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                )
            }
        }
    }
}
