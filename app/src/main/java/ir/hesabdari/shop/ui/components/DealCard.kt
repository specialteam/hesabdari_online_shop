package ir.hesabdari.shop.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hesabdari.shop.data.Deal
import ir.hesabdari.shop.domain.Calc
import ir.hesabdari.shop.domain.Stage
import ir.hesabdari.shop.report.Tone
import ir.hesabdari.shop.util.Fa
import ir.hesabdari.shop.util.Jalali
import kotlin.math.abs

fun stageTone(stage: Stage): Tone = when (stage) {
    Stage.WAIT_PAYMENT -> Tone.LOSS
    Stage.WAIT_DELIVERY -> Tone.MUTED
    Stage.WAIT_SETTLE -> Tone.NEUTRAL
    Stage.SETTLED -> Tone.GAIN
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DealCard(
    deal: Deal,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val unit = LocalMoneyUnit.current
    val profit = Calc.profit(deal)
    val percent = Calc.profitPercent(deal)
    val tone = profit?.let { toneOf(it) } ?: Tone.MUTED
    Surface(
        modifier = modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(end = 10.dp, top = 2.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    deal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val parties = listOf(deal.sellerName, deal.buyerName).filter { it.isNotBlank() }.joinToString(" ← ")
                Text(
                    buildString {
                        append(Jalali.fromEpochDay(deal.dateEpochDay).short())
                        append("  •  ")
                        append(Fa.quantity(deal.quantity)).append(' ').append(deal.unit)
                        if (parties.isNotBlank()) append("  •  ").append(parties)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "خرید " + Fa.number(deal.buyPrice) + (deal.sellPrice?.let { "  ←  فروش " + Fa.number(it) } ?: "  •  فروش نرفته"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (deal.isBrokerage || deal.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        if (deal.isBrokerage) {
                            val stage = Calc.stage(deal)
                            ToneChip("واسطه‌گری", Tone.NEUTRAL, icon = Icons.Rounded.SwapHoriz)
                            ToneChip(stage.label, stageTone(stage))
                        }
                        deal.tags.take(3).forEach { InfoChip("#$it") }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (profit != null) {
                    Text(
                        if (profit < 0) "زیان" else "سود",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Fa.number(abs(profit)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = toneColor(tone),
                        textAlign = TextAlign.End,
                    )
                    Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (percent != null) ToneChip(Fa.percent(abs(percent)), tone, modifier = Modifier.padding(top = 4.dp))
                } else {
                    ToneChip("باز", Tone.MUTED)
                }
            }
        }
    }
}
