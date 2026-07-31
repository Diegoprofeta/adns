package com.eyalm.adns.data.nextdns.connection

import com.eyalm.adns.data.network.ApiClient
import java.io.IOException
import java.security.SecureRandom
import java.util.Locale
import kotlinx.coroutines.CancellationException

sealed interface NextDnsConnectionStatus {
    val probe: NextDnsConnectionProbe?

    data class UsingSelectedProfile(
        override val probe: NextDnsConnectionProbe,
    ) : NextDnsConnectionStatus

    data class UsingDifferentProfile(
        val profileFingerprint: String,
        override val probe: NextDnsConnectionProbe,
    ) : NextDnsConnectionStatus

    data class UsingNextDnsWithoutProfile(
        override val probe: NextDnsConnectionProbe,
    ) : NextDnsConnectionStatus

    data class NotUsingNextDns(
        val resolverName: String?,
        override val probe: NextDnsConnectionProbe,
    ) : NextDnsConnectionStatus

    data class Offline(
        override val probe: NextDnsConnectionProbe? = null,
    ) : NextDnsConnectionStatus

    data class Unavailable(
        val httpStatus: Int? = null,
        override val probe: NextDnsConnectionProbe? = null,
    ) : NextDnsConnectionStatus
}

class NextDnsConnectionRepository(
    private val api: NextDnsConnectionApi = ApiClient.nextDnsConnectionApi,
    private val probeUrl: (String) -> String = ::nextDnsProbeUrl,
) {
    suspend fun detect(
        profileId: String,
        selectedFingerprint: String?,
    ): NextDnsConnectionStatus {
        if (profileId.isBlank()) return NextDnsConnectionStatus.Unavailable()
        return try {
            val response = api.probe(probeUrl(profileId))
            if (!response.isSuccessful) {
                return NextDnsConnectionStatus.Unavailable(response.code())
            }
            val probe = response.body()
                ?: return NextDnsConnectionStatus.Unavailable(response.code())
            when (val status = classifyConnectionProbe(probe, selectedFingerprint)) {
                is NextDnsConnectionStatus.NotUsingNextDns -> status.copy(
                    resolverName = probe.resolver?.let { resolverName(it) }
                )
                else -> status
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            NextDnsConnectionStatus.Offline()
        } catch (_: Exception) {
            NextDnsConnectionStatus.Unavailable()
        }
    }

    private suspend fun resolverName(resolver: String): String? = try {
        api.resolver(resolver)
            .takeIf { it.isSuccessful }
            ?.body()
            ?.name
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }
}

fun classifyConnectionProbe(
    probe: NextDnsConnectionProbe,
    selectedFingerprint: String?,
): NextDnsConnectionStatus = when {
    probe.status.lowercase(Locale.ROOT) == "unconfigured" ->
        NextDnsConnectionStatus.NotUsingNextDns(null, probe)
    !probe.profile.isNullOrBlank() && probe.profile == selectedFingerprint ->
        NextDnsConnectionStatus.UsingSelectedProfile(probe)
    !probe.profile.isNullOrBlank() ->
        NextDnsConnectionStatus.UsingDifferentProfile(probe.profile, probe)
    probe.status.lowercase(Locale.ROOT) in setOf("ok", "mismatch") ->
        NextDnsConnectionStatus.UsingNextDnsWithoutProfile(probe)
    else -> NextDnsConnectionStatus.Unavailable(probe = probe)
}

private const val PROBE_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
private val probeRandom = SecureRandom()

fun nextDnsProbeUrl(profileId: String): String {
    if (profileId.isBlank()) return ""
    val label = buildString(12) {
        repeat(12) { append(PROBE_ALPHABET[probeRandom.nextInt(PROBE_ALPHABET.length)]) }
    }
    return "https://$label-$profileId.test.nextdns.io/"
}
