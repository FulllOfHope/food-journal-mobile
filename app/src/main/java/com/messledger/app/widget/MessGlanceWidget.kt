package com.messledger.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.messledger.app.theme.CardBgLight
import com.messledger.app.theme.DeepSpaceCard
import com.messledger.app.theme.InkBlack
import com.messledger.app.theme.NeonEmber
import com.messledger.app.theme.PaperCream
import com.messledger.app.theme.PencilFaint
import com.messledger.app.theme.StampRed
import com.messledger.app.theme.StarlightWhite
import com.messledger.app.theme.VoidCharcoal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun fixedColor(color: Color): ColorProvider = object : ColorProvider {
    override fun getColor(context: Context): Color = color
}

class MessGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val today = LocalDate.now()
                val isDark = false

                val bg = if (isDark) VoidCharcoal else PaperCream
                val cardBg = if (isDark) DeepSpaceCard else CardBgLight
                val activeColor = if (isDark) NeonEmber else StampRed

                val dayOfMonth = today.dayOfMonth
                val batchText = if (dayOfMonth in 3..17) "Batch 1" else "Batch 2"
                val dateStr = today.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                val isSaturday = today.dayOfWeek == DayOfWeek.SATURDAY

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bg)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateStr,
                            style = TextStyle(
                                color = fixedColor(if (isDark) StarlightWhite else InkBlack),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = batchText,
                            style = TextStyle(
                                color = fixedColor(PencilFaint),
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(12.dp))

                    // Quick Tap Buttons
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MealToggleButton(
                            label = "B",
                            mealType = "BREAKFAST",
                            value = "0.5",
                            isActive = false,
                            color = activeColor,
                            cardBg = cardBg
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        MealToggleButton(
                            label = "L",
                            mealType = "LUNCH",
                            value = if (isSaturday) "2.0" else "1.0",
                            isActive = false,
                            color = activeColor,
                            cardBg = cardBg
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))

                        if (!isSaturday) {
                            MealToggleButton(
                                label = "D",
                                mealType = "DINNER",
                                value = "1.0",
                                isActive = false,
                                color = activeColor,
                                cardBg = cardBg
                            )
                        } else {
                            Box(modifier = GlanceModifier.size(56.dp)) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealToggleButton(
    label: String,
    mealType: String,
    value: String,
    isActive: Boolean,
    color: Color,
    cardBg: Color
) {
    val actionKey = ActionParameters.Key<String>("MEAL_TYPE")

    Box(
        modifier = GlanceModifier
            .size(56.dp)
            .background(if (isActive) color else cardBg)
            .clickable(
                onClick = actionRunCallback<ToggleMealAction>(
                    parameters = actionParametersOf(actionKey to mealType)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = TextStyle(
                    color = fixedColor(if (isActive) PaperCream else InkBlack),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Text(
                text = value,
                style = TextStyle(
                    color = fixedColor(if (isActive) PaperCream.copy(alpha = 0.8f) else PencilFaint),
                    fontSize = 10.sp
                )
            )
        }
    }
}

class ToggleMealAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val mealTypeStr = parameters[ActionParameters.Key<String>("MEAL_TYPE")] ?: return
        
        val db = com.messledger.app.data.MessLedgerDatabase.getDatabase(context)
        val dao = db.messLedgerDao()
        val today = java.time.LocalDate.now().toString()
        val mealType = com.messledger.app.data.MealType.valueOf(mealTypeStr)
        
        val current = dao.getMeal(today, mealType)
        val currentStatus = current?.status ?: true // Default is true (eaten)
        
        dao.logMeal(today, mealType, !currentStatus, System.currentTimeMillis())
        
        MessGlanceWidget().update(context, glanceId)
    }
}