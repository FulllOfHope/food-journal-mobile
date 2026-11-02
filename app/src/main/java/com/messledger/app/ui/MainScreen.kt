package com.messledger.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.messledger.app.data.ThemeMode
import com.messledger.app.ui.navigation.MessNavHost
import com.messledger.app.ui.navigation.NavRoute
import com.messledger.app.ui.theme.Brass
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.ui.theme.NightCanvasBottom
import com.messledger.app.ui.theme.NightCanvasTop
import com.messledger.app.viewmodel.MessViewModel

@Composable
fun MainScreen(
    viewModel: MessViewModel,
    isDark: Boolean,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onResetTheme: () -> Unit
) {
    val navController = rememberNavController()
    val colors = LocalMessColors.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                if (isDark) {
                    // The paper is lit by one warm lamp above the page: a
                    // vertical ink wash plus a single, static pool of light.
                    drawRect(Brush.verticalGradient(listOf(NightCanvasTop, NightCanvasBottom)))
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Brass.copy(alpha = 0.055f), Color.Transparent),
                            center = Offset(size.width / 2f, -size.height * 0.08f),
                            radius = size.width.coerceAtLeast(size.height) * 0.95f
                        )
                    )
                } else {
                    drawRect(colors.background)
                }
            }
    ) {
        // A phone in portrait keeps the familiar bottom bar. Anything wider —
        // tablet, foldable, or a phone on its side — earns a navigation rail.
        val useRail = maxWidth >= 600.dp || maxWidth > maxHeight
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                LedgerTopBar(
                    isDark = isDark,
                    themeMode = themeMode,
                    onToggleTheme = onToggleTheme,
                    onResetTheme = onResetTheme
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = if (isDark) colors.surface.copy(alpha = 0.94f) else Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = NavigationBarDefaults.windowInsets,
                        modifier = if (isDark) {
                            Modifier.drawBehind {
                                drawLine(
                                    color = colors.ruleDark,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        } else {
                            Modifier
                        }
                    ) {
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
                                onClick = { navController.navigateTo(route) },
                                colors = itemColors
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(if (useRail) Modifier.navigationBarsPadding() else Modifier)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .navigationBarsPadding(),
                        containerColor = Color.Transparent,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        NavRoute.values().forEach { route ->
                            NavigationRailItem(
                                icon = { Icon(route.icon, contentDescription = route.title) },
                                label = { Text(route.title) },
                                selected = currentRoute == route.name,
                                onClick = { navController.navigateTo(route) },
                                colors = if (isDark) {
                                    NavigationRailItemDefaults.colors(
                                        selectedIconColor = colors.textPrimary,
                                        selectedTextColor = colors.textPrimary,
                                        indicatorColor = colors.brass.copy(alpha = 0.16f),
                                        unselectedIconColor = colors.textTertiary,
                                        unselectedTextColor = colors.textTertiary
                                    )
                                } else {
                                    NavigationRailItemDefaults.colors()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                MessNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * A slim masthead instead of a full app bar: the journal name at the left,
 * one icon-only lamp switch at the far right. Long-press returns the switch
 * to following the phone's system theme.
 */
@Composable
private fun LedgerTopBar(
    isDark: Boolean,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onResetTheme: () -> Unit
) {
    val colors = LocalMessColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) Color.Transparent else colors.background)
            .statusBarsPadding()
            .drawBehind {
                if (isDark) {
                    // A double hairline, like the ruled edge of a ledger page.
                    val bottom = size.height - 3.dp.toPx()
                    drawLine(
                        color = colors.ruleDark,
                        start = Offset(0f, bottom),
                        end = Offset(size.width, bottom),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = colors.rule.copy(alpha = 0.75f),
                        start = Offset(0f, bottom + 2.dp.toPx()),
                        end = Offset(size.width, bottom + 2.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mess Ledger",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            ThemeSwitch(
                isDark = isDark,
                followsSystem = themeMode == ThemeMode.SYSTEM,
                onToggleTheme = onToggleTheme,
                onResetTheme = onResetTheme
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeSwitch(
    isDark: Boolean,
    followsSystem: Boolean,
    onToggleTheme: () -> Unit,
    onResetTheme: () -> Unit
) {
    val colors = LocalMessColors.current
    val description = if (isDark) "Switch to light mode" else "Switch to dark mode"

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .drawBehind {
                if (isDark) {
                    drawCircle(
                        color = colors.ruleDark,
                        radius = size.minDimension / 2f - 2.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            .combinedClickable(
                onClick = onToggleTheme,
                onLongClick = onResetTheme
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "theme_switch_icon"
        ) { dark ->
            Icon(
                imageVector = if (dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = description,
                tint = if (dark) colors.textPrimary else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        if (followsSystem) {
            // One small brass dot under the icon: still just an icon, but it
            // says the switch is following the phone rather than locked.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-6).dp)
                    .size(3.dp)
                    .background(colors.brass, CircleShape)
            )
        }
    }
}

private fun NavHostController.navigateTo(route: NavRoute) {
    navigate(route.name) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
