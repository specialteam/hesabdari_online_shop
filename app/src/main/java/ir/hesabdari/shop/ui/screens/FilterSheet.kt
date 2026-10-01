package ir.hesabdari.shop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hesabdari.shop.domain.DealFilter
import ir.hesabdari.shop.domain.KindFilter
import ir.hesabdari.shop.domain.Period
import ir.hesabdari.shop.domain.ResultFilter
import ir.hesabdari.shop.domain.SortOrder
import ir.hesabdari.shop.domain.Stage
import ir.hesabdari.shop.ui.components.DateField
import ir.hesabdari.shop.util.Jalali

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    filter: DealFilter,
    allTags: List<String>,
    onChange: ((DealFilter) -> DealFilter) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("فیلتر و مرتب‌سازی", style = MaterialTheme.typography.titleLarge)

            Label("نوع معامله")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KindFilter.entries.forEach { k ->
                    FilterChip(selected = filter.kind == k, onClick = { onChange { it.copy(kind = k) } }, label = { Text(k.label) })
                }
            }

            Label("نتیجه")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultFilter.entries.forEach { r ->
                    FilterChip(selected = filter.result == r, onClick = { onChange { it.copy(result = r) } }, label = { Text(r.label) })
                }
            }

            Label("مرحلهٔ واسطه‌گری")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter.stage == null, onClick = { onChange { it.copy(stage = null) } }, label = { Text("همه") })
                Stage.entries.forEach { s ->
                    FilterChip(
                        selected = filter.stage == s,
                        onClick = { onChange { it.copy(stage = if (it.stage == s) null else s) } },
                        label = { Text(s.label) },
                    )
                }
            }

            if (allTags.isNotEmpty()) {
                Label("برچسب‌ها")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allTags.forEach { t ->
                        FilterChip(
                            selected = t in filter.tags,
                            onClick = { onChange { f -> f.copy(tags = if (t in f.tags) f.tags - t else f.tags + t) } },
                            label = { Text("#$t") },
                        )
                    }
                }
            }

            Label("بازهٔ زمانی")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Period.entries.forEach { p ->
                    val (from, to) = p.range(Jalali.todayEpochDay())
                    FilterChip(
                        selected = filter.fromDay == from && filter.toDay == to,
                        onClick = { onChange { it.copy(fromDay = from, toDay = to) } },
                        label = { Text(p.label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(filter.fromDay, { d -> onChange { it.copy(fromDay = d) } }, "از تاریخ", Modifier.weight(1f), onClear = { onChange { it.copy(fromDay = null) } })
                DateField(filter.toDay, { d -> onChange { it.copy(toDay = d) } }, "تا تاریخ", Modifier.weight(1f), onClear = { onChange { it.copy(toDay = null) } })
            }

            Label("مرتب‌سازی")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOrder.entries.forEach { s ->
                    FilterChip(selected = filter.sort == s, onClick = { onChange { it.copy(sort = s) } }, label = { Text(s.label) })
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("پاک کردن فیلترها") }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("نمایش نتایج") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
}
