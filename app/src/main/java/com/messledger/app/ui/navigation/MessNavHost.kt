package com.messledger.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.messledger.app.ui.screens.LogScreen
import com.messledger.app.ui.screens.MonthScreen
import com.messledger.app.ui.screens.TodayScreen

enum class NavRoute(val title: String, val icon: ImageVector) {
    TODAY("Today", Icons.Rounded.WbSunny),
    LOG("Log", Icons.Rounded.HistoryEdu),
    MONTH("Month", Icons.Rounded.CalendarMonth),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun MessNavHost(
    navController: NavHostController,
    viewModel: com.messledger.app.viewmodel.MessViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.TODAY.name,
        modifier = modifier
    ) {
        composable(NavRoute.TODAY.name) {
            TodayScreen(viewModel)
        }
        composable(NavRoute.LOG.name) {
            LogScreen(viewModel)
        }
        composable(NavRoute.MONTH.name) {
            MonthScreen(viewModel)
        }
        composable(NavRoute.SETTINGS.name) {
            com.messledger.app.ui.screens.SettingsScreen(viewModel)
        }
    }
}
