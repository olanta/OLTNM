package app.oltnm.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.BuildConfig
import app.oltnm.data.api.MastodonApiException
import app.oltnm.data.model.InstanceInfo
import app.oltnm.data.repo.AuthRepository
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginError {
    NotFound,
    InstanceDown,
    Auth,
    Generic
}

data class LoginUiState(
    val instanceInput: String = BuildConfig.DEFAULT_INSTANCE,
    val discovered: InstanceInfo? = null,
    val busy: Boolean = false,
    val authUrl: String? = null,
    val error: LoginError? = null
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onInstanceChange(value: String) {
        _state.update { it.copy(instanceInput = value, error = null) }
    }

    fun discover() {
        if (_state.value.busy) return
        val input = _state.value.instanceInput
        if (input.isBlank()) {
            _state.update { it.copy(error = LoginError.NotFound) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            runCatching { authRepository.discover(input) }
                .onSuccess { info ->
                    _state.update {
                        it.copy(
                            busy = false,
                            discovered = info,
                            instanceInput = info.uri.ifBlank { it.instanceInput }
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(busy = false, error = error.toLoginError()) }
                }
        }
    }

    fun reselectInstance() {
        _state.update { it.copy(discovered = null, error = null) }
    }

    fun beginLogin() {
        val discovered = _state.value.discovered ?: return
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            runCatching {
                authRepository.beginLogin(discovered.uri.ifBlank { _state.value.instanceInput })
            }
                .onSuccess { url ->
                    _state.update { it.copy(busy = false, authUrl = url) }
                }
                .onFailure { error ->
                    _state.update { it.copy(busy = false, error = error.toLoginError()) }
                }
        }
    }

    fun consumeAuthUrl() {
        _state.update { it.copy(authUrl = null) }
    }

    fun completeLogin(code: String) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            runCatching { authRepository.completeLogin(code) }
                .onSuccess {
                    _state.update { it.copy(busy = false) }
                }
                .onFailure {
                    _state.update { it.copy(busy = false, error = LoginError.Auth) }
                }
        }
    }

    fun showError(error: LoginError) {
        _state.update { it.copy(busy = false, error = error) }
    }

    private fun Throwable.toLoginError(): LoginError = when (this) {
        is IllegalArgumentException -> LoginError.NotFound
        is MastodonApiException -> if (statusCode == 404) LoginError.NotFound else LoginError.InstanceDown
        is IOException -> LoginError.InstanceDown
        else -> LoginError.Generic
    }
}
