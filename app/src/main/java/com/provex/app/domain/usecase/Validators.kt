package com.provex.app.domain.usecase

object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    private val urlRegex = Regex("^https?://[^\\s/$.?#].[^\\s]*$", RegexOption.IGNORE_CASE)

    const val MIN_PASSWORD = 8
    const val MIN_JOB_TEXT = 40

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Enter your email address."
        !emailRegex.matches(email.trim()) -> "Enter a valid email address."
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.isEmpty() -> "Enter a password."
        password.length < MIN_PASSWORD -> "Use at least $MIN_PASSWORD characters."
        else -> null
    }

    fun loginPasswordError(password: String): String? = when {
        password.isEmpty() -> "Enter your password."
        else -> null
    }

    fun nameError(name: String): String? = when {
        name.isBlank() -> "Enter your full name."
        name.trim().length < 2 -> "Name must be at least 2 characters."
        else -> null
    }

    fun confirmPasswordError(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Confirm your password."
        confirm != password -> "Passwords do not match."
        else -> null
    }

    fun jobTextError(text: String): String? = when {
        text.isBlank() -> "Paste a job or internship description to analyze."
        text.trim().length < MIN_JOB_TEXT ->
            "That looks too short to analyze. Paste the full description (at least $MIN_JOB_TEXT characters)."
        else -> null
    }

    /** Optional field: blank is valid. */
    fun urlError(url: String): String? =
        if (url.isBlank() || urlRegex.matches(url.trim())) null else "Enter a full link starting with https://"
}
