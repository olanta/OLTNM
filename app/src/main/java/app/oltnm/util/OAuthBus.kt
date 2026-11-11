package app.oltnm.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OAuthBus {
    private val _code = MutableStateFlow<String?>(null)
    val code: StateFlow<String?> = _code.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun deliverCode(value: String) {
        _code.value = value
    }

    fun deliverError(value: String) {
        _error.value = value
    }

    fun consumeCode() {
        _code.value = null
    }

    fun consumeError() {
        _error.value = null
    }
}
