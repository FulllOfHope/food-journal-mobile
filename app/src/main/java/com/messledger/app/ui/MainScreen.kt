package com.messledger.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.messledger.app.ui.navigation.MessNavHost
import com.messledger.app.ui.navigation.NavRoute
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessViewModel

@Composable
fun MainScreen(viewModel: MessViewModel) {
    val navController = rememberNavController()
    val colors = LocalMessColors.current
    val isDark = isSystemInDarkTheme()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = if (isDark) {
                    Modifier.drawBehind {
                        // A hairline instead of a shadow, so the bar reads as a
                        // foot rule in the ledger rather than a floating slab.
                        drawLine(
                            color = colors.rule,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                } else {
                    Modifier
                },
                containerColor = if (isDark) colors.surface else Color.Transparent,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val itemColors = if (isDark) {
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.textPrimary,
                        selectedTextColor = colors.textPrimary,
                        indicatorColor = colors.brass.copy(alpha = 0.16f),
                        unselectedIconColor = colors.textTertiary,
                        unselectedTextColor = colors.textTertiary
                    )
                } else {
                    NavigationBarItemDefaults.colors()
                }

                NavRoute.values().forEach { route ->
                    NavigationBarItem(
                        icon = { Icon(route.icon, contentDescription = route.title) },
                        label = { Text(route.title) },
                        selected = currentRoute == route.name,
                        onClick = {
                            navController.navigate(route.name) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                        colors = itemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        MessNavHost(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
