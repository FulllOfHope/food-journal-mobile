package com.messledger.app.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Honour the system "animation scale" setting. Returning 0 collapses the
 * transition instead of animating nothing at all, which is what reduced motion
 * users expect.
 */
@Composable
fun rememberMotionDuration(standardMs: Int): Int {
    val context = LocalContext.current
    return remember(standardMs) {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        if (scale <= 0f) 0 else (standardMs * scale).toInt().coerceAtLeast(1)
    }
}
