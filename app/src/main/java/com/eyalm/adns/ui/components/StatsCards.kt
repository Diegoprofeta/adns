package com.eyalm.adns.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyalm.adns.data.nextdns.analytics.ListCard
import com.eyalm.adns.data.nextdns.analytics.ListKind
import com.eyalm.adns.data.nextdns.analytics.PercentCard
import com.eyalm.adns.data.nextdns.analytics.StatRow
import com.eyalm.adns.data.nextdns.analytics.fmtPercent
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.ui.screens.HighlightedDomainText
import com.eyalm.adns.viewmodel.nextdns.CardState

@Composable
private fun StatCardShell(
    title: String,
    description: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            if (!description.isNullOrEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun StatCardLoading() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
    }
}

@Composable
private fun StatCardEmpty(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StatDivider(hasIcon: Boolean) {
    HorizontalDivider(
        modifier = Modifier.padding(start = if (hasIcon) 76.dp else 24.dp, end = 24.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun StatRowView(row: StatRow) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (row.icon !is ListIcon.None) {
            ListIconView(row.icon, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            if (row.highlightDomain) {
                HighlightedDomainText(row.title)
            } else {
                Text(
                    row.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            row.subtitle?.let {
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            row.value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun gafamCompanyColor(companyKey: String): Color = when (companyKey.lowercase()) {
    "facebook" -> Color(0xFF3B5998)
    "google" -> Color(0xFF4285F4)
    "microsoft" -> Color(0xFF7FBA00)
    "amazon" -> Color(0xFFFF9900)
    "apple" -> Color(0xFFA2AAAD)
    "others" -> Color(0xFF555555)
    else -> Color(0xFF8E8E93)
}

@Composable
fun GafamDistributionBar(
    rows: List<StatRow>,
    modifier: Modifier = Modifier,
    heightDp: Dp = 10.dp
) {
    val rowValues = rows.map { row ->
        val pct = row.subtitle?.removeSuffix("%")?.toFloatOrNull() ?: 0f
        row to pct
    }.filter { it.second > 0f }

    val totalPct = rowValues.sumOf { it.second.toDouble() }.toFloat()

    if (totalPct <= 0f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(heightDp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        rowValues.forEach { (row, pct) ->
            val weight = (pct / 100f).coerceAtLeast(0.001f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .background(gafamCompanyColor(row.id))
            )
        }
    }
}

@Composable
fun GenericStatsListCard(card: ListCard, state: CardState) {
    StatCardShell(title = card.title(), description = card.description()) {
        when (state) {
            is CardState.Loading -> StatCardLoading()
            is CardState.ListData -> {
                if (state.rows.isEmpty()) {
                    StatCardEmpty(card.emptyText())
                } else {
                    if (card.kind == ListKind.GAFAM) {
                        GafamDistributionBar(
                            rows = state.rows,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    state.rows.forEachIndexed { i, row ->
                        StatRowView(row)
                        if (i < state.rows.lastIndex)
                            StatDivider(hasIcon = row.icon !is ListIcon.None)
                    }
                }
            }
            else -> StatCardEmpty(card.emptyText())
        }
    }
}

@Composable
fun GenericStatsPercentCard(card: PercentCard, state: CardState) {
    StatCardShell(title = card.title(), description = card.description()) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            when (state) {
                is CardState.PercentData -> {
                    LinearProgressIndicator(
                        progress = { (state.percent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        fmtPercent(state.percent),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is CardState.Loading -> StatCardLoading()
                else -> StatCardEmpty(card.emptyText())
            }
        }
    }
}
