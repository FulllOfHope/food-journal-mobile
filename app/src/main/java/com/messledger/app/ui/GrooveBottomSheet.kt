package com.messledger.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.ui.theme.tabularNumbers

/**
 * The one sheet shape used by receipts, range edits and day rules: flat elevated
 * paper, a quiet drag handle, one filled action and at most one text action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrooveBottomSheet(
    title: String,
    copy: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryLabel: String? = null,
    onTertiary: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMessColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.elevated,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = BottomSheetDefaults.ScrimColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(colors.borderStrong, RoundedCornerShape(2.dp))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.tabularNumbers(),
                color = colors.textPrimary
            )
            Text(
                text = copy,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            content()

            if (primaryLabel != null || secondaryLabel != null || tertiaryLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                if (tertiaryLabel != null && onTertiary != null) {
                    TextButton(
                        onClick = onTertiary,
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text(tertiaryLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (secondaryLabel != null && onSecondary != null) {
                        TextButton(
                            onClick = onSecondary,
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text(secondaryLabel, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (primaryLabel != null && onPrimary != null) {
                        Button(
                            onClick = onPrimary,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.tangerine,
                                contentColor = colors.onTangerine
                            ),
                            elevation = null
                        ) {
                            Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
