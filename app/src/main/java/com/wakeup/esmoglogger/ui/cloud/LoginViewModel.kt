package com.wakeup.esmoglogger.ui.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoginButtonEnabled: Boolean = false,
    val loginResult: String? = null
)

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val usernameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isRegisterButtonEnabled: Boolean = false,
    val registerResult: String? = null
)

class LoginViewModel : ViewModel() {
    private val _loginUiState = MutableLiveData(LoginUiState())
    val loginUiState: LiveData<LoginUiState> = _loginUiState

    private val _registerUiState = MutableLiveData(RegisterUiState())
    val registerUiState: LiveData<RegisterUiState> = _registerUiState

    // Login-related functions
    fun updateEmail(email: String) {
        _loginUiState.value = _loginUiState.value?.copy(
            email = email,
            emailError = validateEmail(email),
            isLoginButtonEnabled = isLoginFormValid(email, _loginUiState.value?.password ?: "")
        )
    }

    fun updatePassword(password: String) {
        _loginUiState.value = _loginUiState.value?.copy(
            password = password,
            passwordError = validatePassword(password),
            isLoginButtonEnabled = isLoginFormValid(_loginUiState.value?.email ?: "", password)
        )
    }

    fun login() {
        val email = _loginUiState.value?.email ?: ""
        val password = _loginUiState.value?.password ?: ""

        // Simulate login logic (replace with actual authentication)
        if (email == "test@example.com" && password == "Password123") {
            _loginUiState.value = _loginUiState.value?.copy(loginResult = "Login Successful")
        } else {
            _loginUiState.value = _loginUiState.value?.copy(loginResult = "Invalid email or password")
        }
    }

    // Register-related functions
    fun updateRegisterUsername(username: String) {
        _registerUiState.value = _registerUiState.value?.copy(
            username = username,
            usernameError = validateUsername(username),
            isRegisterButtonEnabled = isRegisterFormValid(
                username,
                _registerUiState.value?.email ?: "",
                _registerUiState.value?.password ?: "",
                _registerUiState.value?.confirmPassword ?: ""
            )
        )
    }

    fun updateRegisterEmail(email: String) {
        _registerUiState.value = _registerUiState.value?.copy(
            email = email,
            emailError = validateEmail(email),
            isRegisterButtonEnabled = isRegisterFormValid(
                _registerUiState.value?.username ?: "",
                email,
                _registerUiState.value?.password ?: "",
                _registerUiState.value?.confirmPassword ?: ""
            )
        )
    }

    fun updateRegisterPassword(password: String) {
        _registerUiState.value = _registerUiState.value?.copy(
            password = password,
            passwordError = validatePassword(password),
            isRegisterButtonEnabled = isRegisterFormValid(
                _registerUiState.value?.username ?: "",
                _registerUiState.value?.email ?: "",
                password,
                _registerUiState.value?.confirmPassword ?: ""
            )
        )
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _registerUiState.value = _registerUiState.value?.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = validateConfirmPassword(confirmPassword, _registerUiState.value?.password ?: ""),
            isRegisterButtonEnabled = isRegisterFormValid(
                _registerUiState.value?.username ?: "",
                _registerUiState.value?.email ?: "",
                _registerUiState.value?.password ?: "",
                confirmPassword
            )
        )
    }

    fun register() {
        val username = _registerUiState.value?.username ?: ""
        val email = _registerUiState.value?.email ?: ""
        val password = _registerUiState.value?.password ?: ""

        // Simulate registration logic (replace with actual API call)
        if (username == "testuser" && email == "test@example.com" && password == "Password123") {
            _registerUiState.value = _registerUiState.value?.copy(registerResult = "Registration Successful")
        } else {
            _registerUiState.value = _registerUiState.value?.copy(registerResult = "Registration Failed")
        }
    }

    private fun validateUsername(username: String): String? {
        return when {
            username.isEmpty() -> "Username cannot be empty"
            username.length < 3 -> "Username must be at least 3 characters"
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isEmpty() -> "Email cannot be empty"
            !email.contains("@") || !email.contains(".") -> "Invalid email format"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isEmpty() -> "Password cannot be empty"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    private fun validateConfirmPassword(confirmPassword: String, password: String): String? {
        return when {
            confirmPassword.isEmpty() -> "Confirm password cannot be empty"
            confirmPassword != password -> "Passwords do not match"
            else -> null
        }
    }

    private fun isLoginFormValid(email: String, password: String): Boolean {
        return validateEmail(email) == null && validatePassword(password) == null
    }

    private fun isRegisterFormValid(username: String, email: String, password: String, confirmPassword: String): Boolean {
        return validateUsername(username) == null &&
                validateEmail(email) == null &&
                validatePassword(password) == null &&
                validateConfirmPassword(confirmPassword, password) == null
    }
}