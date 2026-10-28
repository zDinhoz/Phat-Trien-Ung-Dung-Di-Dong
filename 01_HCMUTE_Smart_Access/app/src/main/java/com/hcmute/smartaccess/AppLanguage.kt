package com.hcmute.smartaccess

enum class AppLanguage {
    ENGLISH,
    VIETNAMESE
}

fun languageFromCode(
    code: String
): AppLanguage {

    return when (code) {

        "vi" -> AppLanguage.VIETNAMESE

        else -> AppLanguage.ENGLISH
    }
}

fun languageCode(
    language: AppLanguage
): String {

    return when (language) {

        AppLanguage.ENGLISH -> "en"

        AppLanguage.VIETNAMESE -> "vi"
    }
}

fun appText(
    language: AppLanguage,
    english: String,
    vietnamese: String
): String {

    return when (language) {

        AppLanguage.ENGLISH ->
            english

        AppLanguage.VIETNAMESE ->
            vietnamese
    }
}