package com.eyalm.adns.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.icu.text.NumberFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.analytics.AnalyticsScope
import com.eyalm.adns.data.nextdns.analytics.ListCard
import com.eyalm.adns.data.nextdns.analytics.ListKind
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.ui.components.DomainTitleText
import com.eyalm.adns.ui.components.ListIconView
import com.eyalm.adns.ui.components.MultiWavyLineChart
import com.eyalm.adns.ui.components.ResourceSettingRow
import com.eyalm.adns.ui.components.StatItemDetailsBottomSheet
import com.eyalm.adns.ui.components.refresh.AdnsPullToRefresh
import com.eyalm.adns.ui.components.rememberMaterialSeriesColors
import com.eyalm.adns.ui.components.segmentPosition
import com.eyalm.adns.ui.theme.pageTitle
import com.eyalm.adns.viewmodel.nextdns.StatsCardDetailEffect
import com.eyalm.adns.viewmodel.nextdns.StatsCardDetailViewModel
import com.eyalm.adns.viewmodel.nextdns.isGraphSupported
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsCardDetailScreen(
    profileId: String,
    card: ListCard,
    scope: AnalyticsScope,
    onBack: () -> Unit,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onScrollVisibilityChange: (Boolean) -> Unit = {},
    statsDetailViewModel: StatsCardDetailViewModel = viewModel(
        key = "stats-card-${card.key}-$profileId"
    ),
) {
    val context = LocalContext.current
    val state by statsDetailViewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    val copiedMessage = stringResource(R.string.copied_to_clipboard)

    BackHandler { onBack() }

    LaunchedEffect(profileId, card.key, scope) {
        statsDetailViewModel.load(profileId, card, scope)
    }

    LaunchedEffect(statsDetailViewModel) {
        statsDetailViewModel.effects.collect { effect ->
            when (effect) {
                is StatsCardDetailEffect.Copy -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("text", effect.text))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }

                is StatsCardDetailEffect.Message -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val nestedScrollConnection = remember(onScrollVisibilityChange) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -4f) {
                    onScrollVisibilityChange(false)
                } else if (delta > 4f) {
                    onScrollVisibilityChange(true)
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0 }
            .collect { isAtTop ->
                if (isAtTop) onScrollVisibilityChange(true)
            }
    }

    val showAppBarTitle by remember {
        derivedStateOf { scrollState.firstVisibleItemIndex > 0 }
    }

    val title = card.title()
    val isBlocked = card.key.contains("blocked", ignoreCase = true)
    val chartColor = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val graphSupported = remember(card.key) { isGraphSupported(card) }
    var selectedSeriesName by remember(card.key) { mutableStateOf<String?>(null) }

    val enabledNames = state.enabledSeriesNames ?: state.series.seriesList.take(3).map { it.name }.toSet()

    val enabledSeriesList = remember(state.series.seriesList, enabledNames) {
        state.series.seriesList.filter { enabledNames.contains(it.name) }
    }

    val m3Colors = rememberMaterialSeriesColors()
    val seriesColorMap = remember(state.series.seriesList, state.items, m3Colors) {
        val map = mutableMapOf<String, Color>()
        state.series.seriesList.forEachIndexed { index, s ->
            val color = m3Colors[index % m3Colors.size]
            map[s.name] = color
            state.items.forEach { item ->
                if (item.title == s.name || item.id == s.name) {
                    map[item.title] = color
                    map[item.id] = color
                }
            }
        }
        state.items.forEachIndexed { index, item ->
            val color = m3Colors[index % m3Colors.size]
            if (item.title !in map) map[item.title] = color
            if (item.id !in map) map[item.id] = color
        }
        map
    }

    AdnsPullToRefresh(
        refreshing = state.refreshing,
        onRefresh = statsDetailViewModel::refresh,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        AnimatedVisibility(
                            visible = showAppBarTitle,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = Locales.getString("global", "back")
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { layoutPadding ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = layoutPadding.calculateTopPadding())
                    .padding(horizontal = 16.dp)
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(
                    bottom = layoutPadding.calculateBottomPadding() + innerPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.pageTitle,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        card.description()?.let { desc ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (graphSupported) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBlocked) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                                val selectedSeries = state.series.seriesList.firstOrNull { it.name == selectedSeriesName }
                                val displayQueries = selectedSeries?.totalQueries ?: enabledSeriesList.sumOf { it.totalQueries }
                                Text(
                                    text = if (state.seriesLoading && displayQueries == 0) "—" else NumberFormat.getNumberInstance(Locale.US).format(displayQueries),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(135.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (enabledSeriesList.isNotEmpty()) {
                                        MultiWavyLineChart(
                                            seriesList = enabledSeriesList,
                                            colors = enabledSeriesList.map { s -> seriesColorMap[s.name] ?: m3Colors[0] },
                                            selectedSeriesName = selectedSeriesName,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (state.seriesLoading) {
                                        CircularWavyProgressIndicator(
                                            modifier = Modifier.size(40.dp),
                                            color = chartColor
                                        )
                                    } else {
                                        Text(
                                            text = card.emptyText().ifEmpty { stringResource(R.string.no_logs_found) },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (state.initialLoading && state.items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = card.emptyText().ifEmpty { stringResource(R.string.no_logs_found) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(
                        count = state.items.size,
                        key = { idx -> "${state.items[idx].id}_$idx" }
                    ) { index ->
                        val item = state.items[index]
                        LaunchedEffect(index, state.items.size, state.hasMorePages, state.loadingNextPage) {
                            if (index >= state.items.size - 5 && state.hasMorePages && !state.loadingNextPage) {
                                statsDetailViewModel.fetchNextPage()
                            }
                        }

                        val isItemEnabled = enabledNames.contains(item.title) || enabledNames.contains(item.id)
                        val itemColor = if (graphSupported && isItemEnabled) {
                            seriesColorMap[item.title] ?: seriesColorMap[item.id]
                        } else null

                        val domain = if (card.kind == ListKind.DOMAINS) item.title else null
                        ResourceSettingRow(
                            title = item.title,
                            titleContent = {
                                if (domain != null) {
                                    DomainTitleText(
                                        domain = domain,
                                        root = null,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                } else {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            description = null,
                            position = segmentPosition(index, state.items.size),
                            alignment = Alignment.CenterVertically,
                            indicatorColor = itemColor,
                            leading = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (graphSupported) {
                                        Spacer(Modifier.width(6.dp))
                                        Checkbox(
                                            checked = isItemEnabled,
                                            onCheckedChange = {
                                                val name = if (state.series.seriesList.any { it.name == item.title }) item.title else item.id
                                                statsDetailViewModel.toggleItemGraph(name)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = itemColor ?: MaterialTheme.colorScheme.primary,
                                                uncheckedColor = MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    } else if (item.icon !is ListIcon.None) {
                                        ListIconView(
                                            icon = item.icon,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            },
                            trailing = {
                                Text(
                                    text = item.value,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isItemEnabled && itemColor != null) itemColor else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                statsDetailViewModel.selectItem(item)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (state.loadingNextPage) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.selectedItem?.let { item ->
        val isItemEnabled = enabledNames.contains(item.title) || enabledNames.contains(item.id)
        val itemColor = if (graphSupported && isItemEnabled) {
            seriesColorMap[item.title] ?: seriesColorMap[item.id]
        } else null

        StatItemDetailsBottomSheet(
            item = item,
            trackerInfo = state.selectedTrackerInfo,
            trackerLoading = state.trackerLoading,
            canToggleGraph = graphSupported,
            isGraphEnabled = isItemEnabled,
            graphColor = itemColor,
            onToggleGraph = {
                val name = if (state.series.seriesList.any { it.name == item.title }) item.title else item.id
                statsDetailViewModel.toggleItemGraph(name)
            },
            onCopy = { statsDetailViewModel.copyText(it) },
            onDismissRequest = { statsDetailViewModel.selectItem(null) }
        )
    }
}

