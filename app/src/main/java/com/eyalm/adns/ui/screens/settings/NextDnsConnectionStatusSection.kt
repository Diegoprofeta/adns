package com.eyalm.adns.ui.screens.settings

import android.R.attr.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.iconColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.api.NextDnsProfile
import com.eyalm.adns.data.nextdns.connection.NextDnsConnectionStatus
import com.eyalm.adns.ui.components.ExpressiveCard
import com.eyalm.adns.ui.components.ExpressiveIcon

@Composable
fun NextDnsConnectionStatusSection(
    status: NextDnsConnectionStatus?,
    profiles: List<NextDnsProfile>,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
) {

    ExpressiveCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = connectionTitle(status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (onRefresh != null) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !refreshing,
                    modifier = Modifier.size(36.dp), // TODO: icon adds top padding
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }


        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = connectionDescription(status, profiles),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun statusIcon(status: NextDnsConnectionStatus?): ImageVector = when (status) {
    null -> Icons.Filled.Refresh
    is NextDnsConnectionStatus.UsingSelectedProfile -> Icons.Filled.CheckCircle
    is NextDnsConnectionStatus.UsingDifferentProfile -> Icons.Filled.SyncProblem
    is NextDnsConnectionStatus.UsingNextDnsWithoutProfile -> Icons.Filled.Info
    is NextDnsConnectionStatus.NotUsingNextDns -> Icons.Filled.Dns
    is NextDnsConnectionStatus.Offline -> Icons.Filled.WifiOff
    is NextDnsConnectionStatus.Unavailable -> Icons.Filled.Error
}

@Composable
private fun statusIconColor(status: NextDnsConnectionStatus?): Color = when (status) {
    null -> MaterialTheme.colorScheme.onSurfaceVariant
    is NextDnsConnectionStatus.UsingSelectedProfile -> MaterialTheme.colorScheme.primary
    is NextDnsConnectionStatus.UsingDifferentProfile -> MaterialTheme.colorScheme.tertiary
    is NextDnsConnectionStatus.UsingNextDnsWithoutProfile -> MaterialTheme.colorScheme.secondary
    is NextDnsConnectionStatus.NotUsingNextDns -> MaterialTheme.colorScheme.error
    is NextDnsConnectionStatus.Offline -> MaterialTheme.colorScheme.onSurfaceVariant
    is NextDnsConnectionStatus.Unavailable -> MaterialTheme.colorScheme.error
}

@Composable
private fun connectionTitle(status: NextDnsConnectionStatus?): String = when (status) {
    null -> stringResource(R.string.nextdns_checking_connection)
    is NextDnsConnectionStatus.UsingSelectedProfile ->
        Locales.getString("setup", "status", "ok", "primary")
    is NextDnsConnectionStatus.UsingNextDnsWithoutProfile ->
        Locales.getString("setup", "status", "mismatch", "withoutConfiguration", "primary")
    is NextDnsConnectionStatus.UsingDifferentProfile ->
        Locales.getString("setup", "status", "mismatch", "withConfiguration", "primary")
    is NextDnsConnectionStatus.NotUsingNextDns ->
        Locales.getString("setup", "status", "unconfigured", "primary")
    is NextDnsConnectionStatus.Offline -> stringResource(R.string.nextdns_connection_offline)
    is NextDnsConnectionStatus.Unavailable ->
        Locales.getString("setup", "status", "error", "primary")
}

@Composable
private fun connectionDescription(
    status: NextDnsConnectionStatus?,
    profiles: List<NextDnsProfile>,
): String = when (status) {
    null -> stringResource(R.string.nextdns_checking_connection_description)
    is NextDnsConnectionStatus.UsingSelectedProfile ->
        Locales.getString("setup", "status", "ok", "secondary")
    is NextDnsConnectionStatus.UsingNextDnsWithoutProfile -> Locales.getString(
        "setup", "status", "mismatch", "withoutConfiguration", "secondary", "clients"
    )
    is NextDnsConnectionStatus.UsingDifferentProfile -> {
        val profileName = profiles.firstOrNull {
            it.fingerprint == status.profileFingerprint
        }?.name
        if (profileName != null) {
            Locales.getPlainString(
                path = arrayOf(
                    "setup",
                    "status",
                    "mismatch",
                    "withConfiguration",
                    "secondary",
                    "sameAccount",
                ),
                values = mapOf("name" to profileName),
            )
        } else {
            Locales.getString(
                "setup",
                "status",
                "mismatch",
                "withConfiguration",
                "secondary",
                "otherAccount",
            )
        }
    }
    is NextDnsConnectionStatus.NotUsingNextDns -> Locales.getPlainString(
        path = arrayOf("setup", "status", "unconfigured", "secondary"),
        values = mapOf(
            "resolver" to (
                status.resolverName ?: status.probe.resolver
                    ?: stringResource(R.string.nextdns_unknown_resolver)
                )
        ),
    )
    is NextDnsConnectionStatus.Offline ->
        stringResource(R.string.nextdns_connection_offline_description)
    is NextDnsConnectionStatus.Unavailable ->
        Locales.getString("setup", "status", "error", "secondary")
}
