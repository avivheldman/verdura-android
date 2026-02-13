package com.verdura.app.util

import android.util.Patterns

object FormValidator {
    fun isValidEmail(email: String) = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun isValidPassword(password: String) = password.length >= 6
    fun isValidDisplayName(name: String) = name.isNotBlank() && name.length >= 2

    data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)

    fun validateEmail(email: String) = when {
        email.isBlank() -> ValidationResult(false, "Email is required")
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult(false, "Please enter a valid email")
        else -> ValidationResult(true)
    }

    fun validatePassword(password: String) = when {
        password.isBlank() -> ValidationResult(false, "Password is required")
        password.length < 6 -> ValidationResult(false, "Password must be at least 6 characters")
        else -> ValidationResult(true)
    }

    fun validateDisplayName(name: String) = when {
        name.isBlank() -> ValidationResult(false, "Display name is required")
        name.length < 2 -> ValidationResult(false, "Display name must be at least 2 characters")
        else -> ValidationResult(true)
    }

    fun validateConfirmPassword(password: String, confirmPassword: String) = when {
        confirmPassword.isBlank() -> ValidationResult(false, "Please confirm your password")
        password != confirmPassword -> ValidationResult(false, "Passwords do not match")
        else -> ValidationResult(true)
    }
}
