package com.messledger.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.messledger.app.MainActivity
import com.messledger.app.data.Config
import com.messledger.app.data.ConfigDataStore
import com.messledger.app.data.MealRecord
import com.messledger.app.data.MealType
import com.messledger.app.data.MessLedgerDatabase
import com.messledger.app.data.ThemeMode
import com.messledger.app.domain.BillingEngine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

private val MEAL_TYPE_KEY = ActionParameters.Key<String>("MEAL_TYPE")

private fun fixedColor(color: Color): ColorProvider = object : ColorProvider {
    override fun getColor(context: Context): Color = color
}

/** One resolved set of widget colours, so the widget matches the app. */
private data class WidgetPalette(
    val background: Color,
    val card: Color,
    val text: Color,
    val secondary: Color,
    val tertiary: Color,
    val eaten: Color,
    val skipped: Color,
    val gold: Color
)

private fun paletteFor(isDark: Boolean): WidgetPalette = if (isDark) {
    WidgetPalette(
        background = Color(0xFF000000),
        card = Color(0xFF0C0E11),
        text = Color(0xFFF2F3F5),
        secondary = Color(0xFFA7ADB8),
        tertiary = Color(0xFF6F7683),
        eaten = Color(0xFF4ADE80),
        skipped = Color(0xFFFF7A6B),
        gold = Color(0xFFE0B25C)
    )
} else {
    WidgetPalette(
        background = Color(0xFFF4F1EA),
        card = Color(0xFFFAF8F2),
        text = Color(0xFF212121),
        secondary = Color(0xFF4A4843),
        tertiary = Color(0xFF8A867E),
        eaten = Color(0xFF5A7A2A),
        skipped = Color(0xFFB3402E),
        gold = Color(0xFF8A6A2F)
    )
}

class MessGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val configStore = ConfigDataStore(context)
        val config = configStore.configFlow.first()
        val themeMode = configStore.themeModeFlow.first()

        val systemDark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isDark = when (themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        val dao = MessLedgerDatabase.getDatabase(context).messLedgerDao()
        val today = LocalDate.now()
        val (cycleStart, cycleEnd) = cycleRange(today)
        val cycleRecords = dao.getMealsBetweenOnce(cycleStart.toString(), cycleEnd.toString())

        val todayMap = cycleRecords
            .filter { it.date == today.toString() }
            .associateBy { it.mealType }

        val lastElapsed = if (today.isBefore(cycleEnd)) today else cycleEnd
        val byDate = cycleRecords.groupBy { LocalDate.parse(it.date) }

        var rebatesUsed = 0.0
        var cycleTotal = 0f
        var guests = 0
        var daysElapsed = 0
        var day = cycleStart
        while (!day.isAfter(lastElapsed)) {
            val dayMap = byDate[day].orEmpty().associateBy { it.mealType }
            rebatesUsed += skippedUnits(day, dayMap)
            guests += byDate[day].orEmpty().sumOf { it.guestCount }
            cycleTotal += BillingEngine.dayLiability(day, dayMap, config)
            daysElapsed++
            day = day.plusDays(1)
        }

        provideContent {
            WidgetContent(
                isDark = isDark,
                today = today,
                todayMap = todayMap,
                rebatesUsed = rebatesUsed,
                allowance = config.allowedMisses,
                cycleTotal = cycleTotal,
                daysElapsed = daysElapsed,
                guests = guests,
                openIntent = Intent(context, MainActivity::class.java)
            )
        }
    }
}

@Composable
private fun WidgetContent(
    isDark: Boolean,
    today: LocalDate,
    todayMap: Map<MealType, MealRecord>,
    rebatesUsed: Double,
    allowance: Int,
    cycleTotal: Float,
    daysElapsed: Int,
    guests: Int,
    openIntent: Intent
) {
    val palette = paletteFor(isDark)
    val isSaturday = today.dayOfWeek == DayOfWeek.SATURDAY
    val batch = if (today.dayOfMonth in 3..17) "Batch 1" else "Batch 2"
    val remaining = (allowance - rebatesUsed).coerceAtLeast(0.0)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(openIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                    style = TextStyle(
                        color = fixedColor(palette.text),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = "$batch · day $daysElapsed",
                    style = TextStyle(
                        color = fixedColor(palette.tertiary),
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
            Text(
                text = "₹%.0f".format(cycleTotal),
                style = TextStyle(
                    color = fixedColor(palette.gold),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rebates ${formatUnits(remaining)}/$allowance",
                style = TextStyle(
                    color = fixedColor(palette.secondary),
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (guests > 0) {
                Text(
                    text = "$guests guest${if (guests == 1) "" else "s"}",
                    style = TextStyle(
                        color = fixedColor(palette.gold),
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
            Text(
                text = "${formatUnits(rebatesUsed)} used",
                style = TextStyle(
                    color = fixedColor(palette.tertiary),
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MealToggle(
                label = "B",
                detail = "0.5",
                eaten = todayMap[MealType.BREAKFAST]?.status ?: true,
                mealType = MealType.BREAKFAST,
                palette = palette,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            MealToggle(
                label = "L",
                detail = if (isSaturday) "2.0" else "1.0",
                eaten = todayMap[MealType.LUNCH]?.status ?: true,
                mealType = MealType.LUNCH,
                palette = palette,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))

            if (isSaturday) {
                ClosedMeal(
                    label = "D",
                    palette = palette,
                    modifier = GlanceModifier.defaultWeight()
                )
            } else {
                MealToggle(
                    label = "D",
                    detail = "1.0",
                    eaten = todayMap[MealType.DINNER]?.status ?: true,
                    mealType = MealType.DINNER,
                    palette = palette,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun MealToggle(
    label: String,
    detail: String,
    eaten: Boolean,
    mealType: MealType,
    palette: WidgetPalette,
    modifier: GlanceModifier = GlanceModifier
) {
    val tint = if (eaten) palette.eaten else palette.skipped
    val background = if (eaten) {
        if (palette.background.luminance() > 0.5f) Color(0xFFDDEBD2) else Color(0xFF12301E)
    } else {
        if (palette.background.luminance() > 0.5f) Color(0xFFF3DFD9) else Color(0xFF351A17)
    }

    Box(
        modifier = modifier
            .height(38.dp)
            .background(background)
            .cornerRadius(8.dp)
            .clickable(
                onClick = actionRunCallback<ToggleMealAction>(
                    parameters = actionParametersOf(MEAL_TYPE_KEY to mealType.name)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = fixedColor(tint),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                maxLines = 1
            )
            Text(
                text = if (eaten) detail else "skip",
                style = TextStyle(
                    color = fixedColor(tint),
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ClosedMeal(
    label: String,
    palette: WidgetPalette,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .background(palette.card)
            .cornerRadius(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = TextStyle(
                    color = fixedColor(palette.tertiary),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = "closed",
                style = TextStyle(
                    color = fixedColor(palette.tertiary),
                    fontSize = 9.sp
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
        val mealTypeStr = parameters[MEAL_TYPE_KEY] ?: return
        val mealType = MealType.valueOf(mealTypeStr)

        val today = LocalDate.now()
        // Saturday dinner is closed; never write a record for it.
        if (mealType == MealType.DINNER && today.dayOfWeek == DayOfWeek.SATURDAY) return

        val dao = MessLedgerDatabase.getDatabase(context).messLedgerDao()
        val current = dao.getMeal(today.toString(), mealType)
        val currentStatus = current?.status ?: true // Default is true (eaten)

        dao.logMeal(today.toString(), mealType, !currentStatus, System.currentTimeMillis())

        MessGlanceWidget().update(context, glanceId)
    }
}

private fun cycleRange(date: LocalDate): Pair<LocalDate, LocalDate> {
    val startMonth = if (date.dayOfMonth < 3) date.minusMonths(1) else date
    val start = startMonth.withDayOfMonth(3)
    val end = start.plusMonths(1).minusDays(1)
    return start to end
}

private fun skippedUnits(date: LocalDate, records: Map<MealType, MealRecord>): Double {
    val isSaturday = date.dayOfWeek == DayOfWeek.SATURDAY
    var units = 0.0
    if (records[MealType.BREAKFAST]?.status == false) units += 0.5
    if (records[MealType.LUNCH]?.status == false) units += if (isSaturday) 2.0 else 1.0
    if (records[MealType.DINNER]?.status == false && !isSaturday) units += 1.0
    return units
}

private fun formatUnits(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
