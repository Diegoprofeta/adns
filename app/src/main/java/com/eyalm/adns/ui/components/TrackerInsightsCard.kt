package com.eyalm.adns.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.api.TrackerInfo
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import java.util.Locale

@Composable
fun TrackerInsightsCard(
    tracker: TrackerInfo,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val categoryDesc = remember(tracker.category) {
        val cat = tracker.category?.lowercase(Locale.US) ?: ""
        if (cat.isNotEmpty()) {
            val localized = Locales.getString("logs", "log", "insights", "categories", cat, "description")
            if (localized.isNotEmpty() && !localized.startsWith("[missing:")) localized else null
        } else {
            null
        }
    }

    val prevalence = tracker.prevalence ?: 0f
    val prevalentSuffixFormat = stringResource(R.string.prevalent_format)
    val intensityLabel = remember(prevalence, prevalentSuffixFormat) {
        val key = when {
            prevalence >= 0.20f -> "dangerously"
            prevalence >= 0.10f -> "extremely"
            prevalence >= 0.05f -> "very"
            prevalence >= 0.01f -> "commonly"
            else -> "relatively"
        }
        val label = Locales.getString("logs", "log", "insights", "prevalence", "intensity", key)
        if (label.isNotEmpty() && !label.startsWith("[missing:")) label else String.format(prevalentSuffixFormat, key.replaceFirstChar { it.uppercase(Locale.US) })
    }

    val pctFormatted = remember(prevalence) {
        String.format(Locale.US, "%.2f%%", prevalence * 100f)
    }

    ExpressiveCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = tracker.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!categoryDesc.isNullOrBlank()) {
                Text(
                    text = categoryDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            tracker.website?.takeIf(String::isNotBlank)?.let { website ->
                val displayUrl =
                    website.removePrefix("https://").removePrefix("http://").removeSuffix("/")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                val uri =
                                    if (website.startsWith("http")) website.toUri() else "https://$website".toUri()
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (_: Exception) {
                            }
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListIconView(
                        icon = nextDnsFaviconUrl(displayUrl)?.let(ListIcon::Url)
                            ?: ListIcon.None,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = displayUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (prevalence > 0f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.prevalence_tracks_summary, intensityLabel, pctFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    tracker.company?.let { company ->

        val companyName = company.name ?: tracker.name
        val companyDesc = company.description
        val privacyUrl = company.privacyUrl

        if (!companyDesc.isNullOrBlank() || !privacyUrl.isNullOrBlank()) {
            ExpressiveCard(
                modifier = modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.owned_by, companyName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!companyDesc.isNullOrBlank()) {
                        Text(
                            text = companyDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }

                    if (!privacyUrl.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val uri =
                                            if (privacyUrl.startsWith("http")) privacyUrl.toUri() else "https://$privacyUrl".toUri()
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: Exception) {
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.privacy_policy),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                }
            }
        }
    }
}

