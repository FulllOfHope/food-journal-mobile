package com.messledger.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.messledger.app.R
import com.messledger.app.domain.GitHubUpdateChecker
import com.messledger.app.domain.UpdateInfo
import com.messledger.app.ui.theme.LocalMessColors
import com.messledger.app.ui.theme.messSurface

/**
 * The update sheet: warm paper, one hairline-bordered note, one tangerine
 * action. Not a stock AlertDialog — it uses the same sheet as receipts.
 */
@Composable
fun UpdateSheet(
    update: UpdateInfo,
    onDismiss: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = LocalMessColors.current
    val context = LocalContext.current
    val notes = remember(update.notes) { GitHubUpdateChecker.stripMarkdown(update.notes) }

    GrooveBottomSheet(
        title = "A new page for your journal",
        copy = "Version ${update.versionName} is ready to install. Here is what changed.",
        onDismiss = onDismiss,
        primaryLabel = "Download update",
        onPrimary = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, update.apkUrl.toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            onDismiss()
        },
        secondaryLabel = "Later",
        onSecondary = onLater,
        tertiaryLabel = "Skip this version",
        onTertiary = onSkip
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_groove_foreground),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(48.dp)
            )
            Column {
                Text(
                    text = "Update available",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(Color.Transparent, RoundedCornerShape(999.dp))
                        .border(1.dp, colors.tangerine, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v${update.versionName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.tangerine
                    )
                }
            }
        }

        Text(
            text = "RELEASE NOTES",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState())
                .messSurface()
                .padding(14.dp)
        ) {
            Text(
                text = notes.ifBlank { "No release notes were provided." },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        Text(
            text = "Checked with GitHub. No analytics or tracking.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary
        )
    }
}
