package com.messledger.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import com.messledger.app.theme.*

@Composable
fun DaySummaryBar(
    currentDate: LocalDate,
    consumed: Double,
    allowanceTotal: Int = 25,
    modifier: Modifier = Modifier
) {
    val dayOfMonth = currentDate.dayOfMonth
    val batch = if (dayOfMonth in 3..17) 1 else 2
    
    val over = (consumed - allowanceTotal).coerceAtLeast(0.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .messLedgerBorder(isActive = false)
            .messLedgerBackground()
            .padding(16.dp)
    ) {
        Text(
            text = "Batch $batch",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Rebates Used:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$consumed / $allowanceTotal",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        if (over > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Excess Unrebated Skips:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = StampRed
                )
                Text(
                    text = "$over",
                    style = MaterialTheme.typography.bodyLarge,
                    color = StampRed
                )
            }
        }
    }
}
