package com.messledger.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp

/** Flat 12dp surface. No grain, no wash, no shadow. */
fun Modifier.messLedgerBackground(): Modifier = composed {
    val colors = LocalMessColors.current
    this.background(colors.surface, RoundedCornerShape(12.dp))
}

/** One hairline border. Active state uses tangerine, never brass. */
fun Modifier.messLedgerBorder(isActive: Boolean = false): Modifier = composed {
    val colors = LocalMessColors.current
    val tint = if (isActive) colors.tangerine else colors.border
    this.border(1.dp, tint, RoundedCornerShape(12.dp))
}

/** Surface + border in the correct draw order. */
@Composable
fun Modifier.messSurface(isActive: Boolean = false): Modifier =
    this.messLedgerBackground().messLedgerBorder(isActive)
