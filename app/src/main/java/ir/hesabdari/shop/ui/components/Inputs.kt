package ir.hesabdari.shop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali
import ir.hesabdari.shop.util.JalaliDate

/** Shows `1250000` as `۱٬۲۵۰٬۰۰۰` while the field keeps editing plain digits. */
private object ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val sb = StringBuilder()
        val toTransformed = IntArray(raw.length + 1)
        for (i in raw.indices) {
            if (i > 0 && (raw.length - i) % 3 == 0) sb.append(Fa.THOUSANDS)
            toTransformed[i] = sb.length
            sb.append(raw[i])
        }
        toTransformed[raw.length] = sb.length
        val out = sb.toString()
        val toOriginal = IntArray(out.length + 1)
        var digitsSeen = 0
        for (t in out.indices) {
            toOriginal[t] = digitsSeen
            if (out[t].toString() != Fa.THOUSANDS) digitsSeen++
        }
        toOriginal[out.length] = raw.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = toTransformed[offset.coerceIn(0, raw.length)]
            override fun transformedToOriginal(offset: Int): Int = toOriginal[offset.coerceIn(0, out.length)]
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

/** Integer money input. [value] is the plain digits string (Latin digits). */
@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String = LocalMoneyUnit.current,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(Fa.digitsOnly(it).take(15).trimStart('0')) },
        modifier = modifier,
        label = { Text(label) },
        suffix = { Text(suffix, style = MaterialTheme.typography.labelMedium) },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = true,
        visualTransformation = ThousandsTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.medium,
    )
}

/** Decimal input (quantity). */
@Composable
fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val cleaned = Fa.latinDigits(raw).replace('٫', '.').replace(',', '.')
                .filter { it.isDigit() || it == '.' }
            if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 12) onValueChange(cleaned)
        },
        modifier = modifier,
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
    )
}

/** Read-only field that opens the Jalali date picker. */
@Composable
fun DateField(
    epochDay: Long?,
    onPick: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    Box(modifier) {
        OutlinedTextField(
            value = epochDay?.let { Jalali.fromEpochDay(it).full() } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("انتخاب تاریخ") },
            leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null) },
            trailingIcon = {
                if (epochDay != null && onClear != null) {
                    TextButton(onClick = onClear) { Text("حذف") }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        // Transparent overlay so the whole field is tappable (a read-only text field swallows clicks).
        Box(
            Modifier.matchParentSize().clickable(interactionSource = interaction, indication = null) { open = true },
        )
    }
    if (open) {
        JalaliDatePickerDialog(
            initialEpochDay = epochDay ?: Jalali.todayEpochDay(),
            onDismiss = { open = false },
            onPick = { open = false; onPick(it) },
        )
    }
}

private enum class PickerMode { DAYS, YEARS }

@Composable
fun JalaliDatePickerDialog(initialEpochDay: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val initial = remember(initialEpochDay) { Jalali.fromEpochDay(initialEpochDay) }
    var selected by remember { mutableStateOf(initial) }
    var viewYear by rememberSaveable { mutableIntStateOf(initial.year) }
    var viewMonth by rememberSaveable { mutableIntStateOf(initial.month) }
    var mode by remember { mutableStateOf(PickerMode.DAYS) }
    val today = remember { Jalali.today() }

    fun shiftMonth(delta: Int) {
        var m = viewMonth + delta
        var y = viewYear
        while (m > 12) { m -= 12; y++ }
        while (m < 1) { m += 12; y-- }
        viewYear = y
        viewMonth = m
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.padding(16.dp)) {
                // Header with the current selection
                Column(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primaryContainer).padding(14.dp),
                ) {
                    Text(
                        selected.weekdayName + "  " + selected.long(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "میلادی: " + Fa.digits(Jalali.gregorian(selected.epochDay)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { shiftMonth(-1) }, enabled = mode == PickerMode.DAYS) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "ماه قبل")
                    }
                    TextButton(
                        onClick = { mode = if (mode == PickerMode.DAYS) PickerMode.YEARS else PickerMode.DAYS },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            Jalali.monthNames[viewMonth - 1] + " " + Fa.digits("$viewYear"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = { shiftMonth(1) }, enabled = mode == PickerMode.DAYS) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "ماه بعد")
                    }
                }
                if (mode == PickerMode.YEARS) {
                    YearGrid(current = viewYear, onPick = { viewYear = it; mode = PickerMode.DAYS })
                } else {
                    MonthGrid(
                        year = viewYear,
                        month = viewMonth,
                        selected = selected,
                        today = today,
                        onPick = { selected = it },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            selected = today
                            viewYear = today.year
                            viewMonth = today.month
                            mode = PickerMode.DAYS
                        },
                    ) { Text("امروز") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Button(onClick = { onPick(selected.epochDay) }) { Text("تأیید") }
                }
            }
        }
    }
}

@Composable
private fun YearGrid(current: Int, onPick: (Int) -> Unit) {
    val years = remember { (1380..1460).toList() }
    val state = rememberLazyGridState()
    LaunchedEffect(Unit) {
        val idx = years.indexOf(current).coerceAtLeast(0)
        state.scrollToItem((idx - 3).coerceAtLeast(0))
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = state,
        modifier = Modifier.fillMaxWidth().height(264.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(years) { y ->
            val sel = y == current
            Box(
                Modifier.height(40.dp).clip(CircleShape)
                    .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onPick(y) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    Fa.digits("$y"),
                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(year: Int, month: Int, selected: JalaliDate, today: JalaliDate, onPick: (JalaliDate) -> Unit) {
    val length = Jalali.monthLength(year, month)
    val offset = Jalali.weekdayIndex(Jalali.toEpochDay(year, month, 1))
    Column(Modifier.fillMaxWidth().height(264.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Jalali.weekdayShort.forEachIndexed { i, n ->
                Text(
                    n,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (i == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val rows = (offset + length + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).fillMaxSize().padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..length) {
                            val d = JalaliDate(year, month, day)
                            val isSel = d == selected
                            val isToday = d == today
                            Box(
                                Modifier.aspectRatio(1f).clip(CircleShape)
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .then(
                                        if (isToday && !isSel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape) else Modifier,
                                    )
                                    .clickable { onPick(d) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    Fa.digits("$day"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSel -> MaterialTheme.colorScheme.onPrimary
                                        c == 6 -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
