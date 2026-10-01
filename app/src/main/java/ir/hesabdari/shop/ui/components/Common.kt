package ir.hesabdari.shop.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.widget.Toast
import ir.hesabdari.shop.report.Block
import ir.hesabdari.shop.report.Exporter
import ir.hesabdari.shop.report.ReportDoc
import ir.hesabdari.shop.report.Tone
import ir.hesabdari.shop.ui.theme.semantic
import kotlinx.coroutines.launch

/** Money unit label ("تومان" / "ریال") for the whole UI. */
val LocalMoneyUnit = compositionLocalOf { "تومان" }

@Composable
fun toneColor(tone: Tone): Color = when (tone) {
    Tone.GAIN -> MaterialTheme.semantic.gain
    Tone.LOSS -> MaterialTheme.semantic.loss
    Tone.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
    Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
}

fun toneOf(value: Long): Tone = when {
    value > 0 -> Tone.GAIN
    value < 0 -> Tone.LOSS
    else -> Tone.NEUTRAL
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = container)
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(4.dp).height(18.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = null,
) {
    Surface(modifier = modifier, shape = CircleShape, color = container, contentColor = content) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ToneChip(text: String, tone: Tone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val (bg, fg) = when (tone) {
        Tone.GAIN -> MaterialTheme.semantic.gainContainer to MaterialTheme.semantic.onGainContainer
        Tone.LOSS -> MaterialTheme.semantic.lossContainer to MaterialTheme.semantic.onLossContainer
        Tone.MUTED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        Tone.NEUTRAL -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    InfoChip(text, modifier, bg, fg, icon)
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.NEUTRAL,
    hint: String? = null,
    container: Color = MaterialTheme.colorScheme.surface,
) {
    AppCard(modifier, container = container) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = toneColor(tone))
        if (hint != null) {
            Spacer(Modifier.height(2.dp))
            Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun KeyValueRow(label: String, value: String, tone: Tone = Tone.NEUTRAL, bold: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = toneColor(tone),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(84.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(8.dp))
            action()
        }
    }
}

/** Top-bar action offering "share text / copy / PDF / print" for the current screen. */
@Composable
fun ExportAction(buildDoc: () -> ReportDoc) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun pdf(action: (java.io.File, ReportDoc) -> Unit) {
        val doc = buildDoc()
        scope.launch {
            try {
                val file = Exporter.createPdf(context, doc)
                action(file, doc)
            } catch (e: Exception) {
                Toast.makeText(context, "ساخت PDF با خطا روبه‌رو شد", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.Print, contentDescription = "خروجی و چاپ") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("اشتراک‌گذاری به صورت متن") },
                leadingIcon = { Icon(Icons.Rounded.IosShare, null) },
                onClick = { open = false; Exporter.shareText(context, buildDoc()) },
            )
            DropdownMenuItem(
                text = { Text("کپی متن") },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                onClick = {
                    open = false
                    Exporter.copyText(context, buildDoc())
                    Toast.makeText(context, "متن کپی شد", Toast.LENGTH_SHORT).show()
                },
            )
            DropdownMenuItem(
                text = { Text("خروجی PDF (ذخیره / ارسال)") },
                leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, null) },
                onClick = { open = false; pdf { f, d -> Exporter.sharePdf(context, f, d.title) } },
            )
            DropdownMenuItem(
                text = { Text("چاپ") },
                leadingIcon = { Icon(Icons.Rounded.Print, null) },
                onClick = { open = false; pdf { f, d -> Exporter.printPdf(context, f, d.title) } },
            )
        }
    }
}

/** On-screen rendering of a [ReportDoc] (used for the invoice preview). */
@Composable
fun DocView(doc: ReportDoc, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(doc.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        doc.subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        for (b in doc.blocks) {
            when (b) {
                is Block.Heading -> Text(b.text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                is Block.Paragraph -> Text(
                    b.text,
                    style = if (b.small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = if (b.small) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textAlign = if (b.center) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                is Block.KeyValues -> Column {
                    b.rows.forEach { KeyValueRow(it.label, it.value, tone = it.tone, bold = it.bold) }
                }
                is Block.Table -> DocTable(b)
                Block.Spacer -> Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun DocTable(t: Block.Table) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxWidth()) {
        @Composable
        fun row(cells: List<String>, bold: Boolean, bg: Color) {
            Row(Modifier.fillMaxWidth().background(bg).padding(vertical = 6.dp, horizontal = 2.dp)) {
                cells.forEachIndexed { i, c ->
                    Text(
                        c,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(t.weights[i]).padding(horizontal = 2.dp),
                    )
                }
            }
        }
        row(t.headers, true, MaterialTheme.colorScheme.primaryContainer)
        t.rows.forEach {
            row(it, false, Color.Transparent)
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(line))
        }
        t.footer?.let { row(it, true, MaterialTheme.colorScheme.primaryContainer) }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))
