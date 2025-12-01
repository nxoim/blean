package com.nxoim.blean.ui.postUi.parts

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun PostDate(
    date: Instant,
    modifier: Modifier = Modifier,
    full: Boolean = false,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    // TODO time zone must be decided in a global configuration passed
    //  via a composition local or something
    Text(
        if (full) date.toFullDateString() else date.toShortRelativeString(),
        style = style,
        color = color,
        modifier = modifier
    )
}

@OptIn(ExperimentalTime::class)
private fun formatLdt(
    ldt: LocalDateTime,
    monthNames: MonthNames,
    monthNumeric: Boolean = false,
    includeYear: Boolean = false,
    fullMonth: Boolean = false
): String {
    val fmt = LocalDateTime.Format {
        day()
        if (monthNumeric) {
            char('.'); monthNumber(); char('.')
            if (includeYear) { year(); chars(" ") }
        } else {
            chars(" ")
            monthName(if (fullMonth) MonthNames.ENGLISH_FULL else monthNames)
            if (includeYear) { chars(" "); year(); chars(" ") }
        }
        chars(" ")
        hour(); char(':'); minute()
    }
    return fmt.format(ldt)
}

private val defaultRecentFormat = LocalDateTime.Format {
    day()
    chars(" ")
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    chars(" ")
    hour()
    char(':')
    minute()
}

@OptIn(ExperimentalTime::class)
fun Instant.toShortRelativeString(
    nowInstant: Instant = Clock.System.now(),
    tz: TimeZone = TimeZone.currentSystemDefault(),
    monthNames: MonthNames = MonthNames.ENGLISH_ABBREVIATED
): String {
    val diffSec = (nowInstant.toEpochMilliseconds() - this.toEpochMilliseconds()) / 1000
    if (diffSec < 0) {
        // future => simple absolute recent-format
        return defaultRecentFormat.format(this.toLocalDateTime(tz))
    }

    when {
        diffSec < 5 -> return "now"
        diffSec < 60 -> return "${diffSec}sec ago"
        diffSec < 3600 -> return "${diffSec / 60}min ago"
        diffSec < 86400 -> return "${diffSec / 3600}h ago"
        else -> {
            val ldt = this.toLocalDateTime(tz)
            val nowLdt = nowInstant.toLocalDateTime(tz)
            val daysBetween = nowLdt.date.daysUntil(ldt.date).absoluteValue

            // within last 7 days and same year -> "26 Aug 21:43"
            if (daysBetween <= 7 && nowLdt.year == ldt.year) {
                return formatLdt(ldt, monthNames, monthNumeric = false, includeYear = false)
            }

            // different year -> "26.8.2020 21:43"
            if (ldt.year != nowLdt.year) {
                return formatLdt(ldt, monthNames, monthNumeric = true, includeYear = true)
            }

            // same year but older than 7 days -> "26 Aug 21:43"
            return formatLdt(ldt, monthNames, monthNumeric = false, includeYear = false)
        }
    }
}

@OptIn(ExperimentalTime::class)
fun Instant.toFullDateString(
    tz: TimeZone = TimeZone.currentSystemDefault(),
    monthNames: MonthNames = MonthNames.ENGLISH_FULL
): String {
    val ldt = this.toLocalDateTime(tz)
    return formatLdt(ldt, monthNames, monthNumeric = false, includeYear = true, fullMonth = true)
}
