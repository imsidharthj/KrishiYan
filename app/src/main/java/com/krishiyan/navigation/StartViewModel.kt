package com.krishiyan.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishiyan.auth.AuthRepository
import com.krishiyan.profile.ProfileRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

enum class StartState { Loading, LoggedOut, NeedsProfile, Ready, Offline }

class StartViewModel(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StartState.Loading)
    val state: StateFlow<StartState> = _state

    init {
        viewModelScope.launch {
            auth.sessionStatus
                .distinctUntilChangedBy { (it as? SessionStatus.Authenticated)?.session?.user?.id ?: it::class }
                .collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> checkProfile()
                        is SessionStatus.NotAuthenticated -> _state.value = StartState.LoggedOut
                        is SessionStatus.RefreshFailure -> _state.value = StartState.Offline
                        is SessionStatus.Initializing -> _state.value = StartState.Loading
                    }
                }
        }
    }

    fun checkProfile() {
        _state.value = StartState.Loading
        viewModelScope.launch {
            _state.value = try {
                if (profiles.getMyProfile() == null) StartState.NeedsProfile else StartState.Ready
            } catch (e: Exception) {
                StartState.Offline
            }
        }
    }

    fun onProfileCreated() {
        _state.value = StartState.Ready
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}
