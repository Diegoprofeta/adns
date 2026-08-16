package com.eyalm.adns.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyalm.adns.data.nextdns.analytics.ListCard
import com.eyalm.adns.data.nextdns.analytics.ListKind
import com.eyalm.adns.data.nextdns.analytics.PercentCard
import com.eyalm.adns.data.nextdns.analytics.StatRow
import com.eyalm.adns.data.nextdns.analytics.fmtPercent
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.viewmodel.nextdns.CardState

@Composable
private fun StatCardShell(
    title: String,
    description: String?,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
    )
    val cardShape = RoundedCornerShape(20.dp)

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),

            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!description.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        content()
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            colors = cardColors
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                cardContent()
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            colors = cardColors
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                cardContent()
            }
        }
    }
}

@Composable
private fun StatCardLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
    }
}

@Composable
private fun StatCardEmpty(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun GenericStatsListCard(
    card: ListCard,
    state: CardState,
    onItemClick: ((StatRow) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    StatCardShell(
        title = card.title(),
        description = card.description(),
        onClick = onClick
    ) {
        when (state) {
            is CardState.Loading -> StatCardLoading()
            is CardState.ListData -> {
                if (state.rows.isEmpty()) {
                    StatCardEmpty(card.emptyText())
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.rows.forEachIndexed { index, row ->
                            val domain = if (card.kind == ListKind.DOMAINS) row.title else null
                            ResourceSettingRow(
                                title = row.title,
                                selected = true,
                                titleContent = {
                                    if (domain != null) {
                                        DomainTitleText(
                                             domain = domain,
                                             root = null,
                                             style = MaterialTheme.typography.titleMedium,
                                        )
                                    } else {
                                        Text(
                                             text = row.title,
                                             style = MaterialTheme.typography.titleMedium,
                                             maxLines = 1,
                                             overflow = TextOverflow.Ellipsis,
                                             color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                description = null,
                                position = segmentPosition(index, state.rows.size),
                                alignment = Alignment.CenterVertically,
                                leading = if (row.icon !is ListIcon.None) {
                                    {
                                        ListIconView(
                                             icon = row.icon,
                                             modifier = Modifier.size(28.dp)
                                        )
                                    }
                                } else null,
                                trailing = {
                                    Text(
                                         text = row.value,
                                         style = MaterialTheme.typography.titleSmall,
                                         fontWeight = FontWeight.Bold,
                                         color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                selectedColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                onClick = {
                                    if (onItemClick != null) {
                                        onItemClick(row)
                                    } else if (onClick != null) {
                                        onClick()
                                    }
                                }
                            )
                        }
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
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
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
