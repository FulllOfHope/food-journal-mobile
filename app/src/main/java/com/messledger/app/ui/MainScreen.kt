package com.messledger.app.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.messledger.app.data.ThemeMode
import com.messledger.app.updates.UpdateManager
import com.messledger.app.ui.navigation.MessNavHost
import com.messledger.app.ui.navigation.NavRoute
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.viewmodel.MessViewModel

@Composable
fun MainScreen(
    viewModel: MessViewModel,
    isDark: Boolean,
    themeMode: ThemeMode,
    updateManager: UpdateManager,
    onToggleTheme: () -> Unit,
    onResetTheme: () -> Unit
) {
    val navController = rememberNavController()
    val colors = LocalMessColors.current
    val context = LocalContext.current

    val availableUpdate by updateManager.availableUpdate.collectAsState()
    val sheetVisible by updateManager.sheetVisible.collectAsState()
    val needsPermission by updateManager.needsNotificationPermission.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> updateManager.onNotificationPermissionResult(granted) }

    LaunchedEffect(needsPermission) {
        if (needsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            updateManager.onPermissionRequestShown()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        updateManager.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
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
                        containerColor = colors.surface,
                        tonalElevation = 0.dp,
                        windowInsets = NavigationBarDefaults.windowInsets,
                        modifier = Modifier.drawBehind {
                            drawLine(
                                color = colors.border,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    ) {
                        NavRoute.values().forEach { route ->
                            val selected = currentRoute == route.name
                            NavigationBarItem(
                                icon = { NavIcon(route, selected) },
                                label = { Text(route.title, maxLines = 1) },
                                selected = selected,
                                alwaysShowLabel = true,
                                onClick = { navController.navigateTo(route) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.tangerine,
                                    selectedTextColor = colors.tangerine,
                                    unselectedIconColor = colors.textTertiary,
                                    unselectedTextColor = colors.textTertiary,
                                    indicatorColor = Color.Transparent
                                )
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
                            .navigationBarsPadding()
                            .drawBehind {
                                drawLine(
                                    color = colors.border,
                                    start = Offset(size.width - 1.dp.toPx(), 0f),
                                    end = Offset(size.width - 1.dp.toPx(), size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            },
                        containerColor = colors.surface,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        NavRoute.values().forEach { route ->
                            val selected = currentRoute == route.name
                            NavigationRailItem(
                                icon = { NavIcon(route, selected) },
                                label = { Text(route.title, maxLines = 1) },
                                selected = selected,
                                alwaysShowLabel = true,
                                onClick = { navController.navigateTo(route) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.tangerine,
                                    selectedTextColor = colors.tangerine,
                                    unselectedIconColor = colors.textTertiary,
                                    unselectedTextColor = colors.textTertiary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                MessNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    updateManager = updateManager,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (sheetVisible && availableUpdate != null) {
        UpdateSheet(
            update = availableUpdate!!,
            onDismiss = { updateManager.dismissSheet() },
            onLater = { updateManager.later() },
            onSkip = { updateManager.skipThisVersion() }
        )
    }
}

/** Tangerine icon and text plus one small dot; no filled pill. */
@Composable
private fun NavIcon(route: NavRoute, selected: Boolean) {
    val colors = LocalMessColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(route.icon, contentDescription = route.title, modifier = Modifier.size(20.dp))
        if (selected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .background(colors.tangerine, CircleShape)
            )
        }
    }
}

/**
 * A slim masthead: the journal name at the left, one hairline rule under it in
 * both modes, and one icon-only lamp switch at the far right. Long-press
 * returns the switch to following the phone's system theme.
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
            .background(colors.background)
            .statusBarsPadding()
            .drawBehind {
                val bottom = size.height - 1.dp.toPx()
                drawLine(
                    color = colors.border,
                    start = Offset(0f, bottom),
                    end = Offset(size.width, bottom),
                    strokeWidth = 1.dp.toPx()
                )
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
                text = "Food Journal",
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
                drawCircle(
                    color = colors.borderStrong,
                    radius = size.minDimension / 2f - 2.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-6).dp)
                    .size(3.dp)
                    .background(colors.textTertiary, CircleShape)
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
