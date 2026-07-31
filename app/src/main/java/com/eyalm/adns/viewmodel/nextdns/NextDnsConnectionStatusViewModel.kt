package com.eyalm.adns.viewmodel.nextdns

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.data.nextdns.api.NextDnsProfile
import com.eyalm.adns.data.nextdns.connection.NextDnsConnectionRepository
import com.eyalm.adns.data.nextdns.connection.NextDnsConnectionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NextDnsConnectionStatusUiState(
    val connection: NextDnsConnectionStatus? = null,
    val refreshing: Boolean = false,
)

class NextDnsConnectionStatusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NextDnsConnectionRepository()
    private val _state = MutableStateFlow(NextDnsConnectionStatusUiState())
    val state = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var lastRefreshAt: Long? = null

    fun refresh(
        selectedProfile: NextDnsProfile?,
        force: Boolean = false,
    ) {
        val now = SystemClock.elapsedRealtime()
        if (!force && lastRefreshAt?.let { now - it < REFRESH_DEBOUNCE_MS } == true) return
        if (!force && refreshJob?.isActive == true) return
        lastRefreshAt = now
        refreshJob?.cancel()
        _state.value = _state.value.copy(refreshing = true)
        refreshJob = viewModelScope.launch {
            val connection = selectedProfile?.let {
                repository.detect(it.id, it.fingerprint)
            } ?: NextDnsConnectionStatus.Unavailable()
            _state.value = NextDnsConnectionStatusUiState(connection = connection)
        }
    }

    private companion object {
        const val REFRESH_DEBOUNCE_MS = 5_000L
    }
}
