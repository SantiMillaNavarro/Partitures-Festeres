package com.partituresfesteres.app.data

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LanguageManager {
    const val LANGUAGE_VALENCIAN = "ca-ES-valencia"
    const val LANGUAGE_SPANISH = "es-ES"

    fun wrap(context: Context): Context {
        val prefs = context.getSharedPreferences(SettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
        val language = prefs.getString(SettingsStore.KEY_APP_LANGUAGE, LANGUAGE_VALENCIAN)
            ?.takeIf { it == LANGUAGE_VALENCIAN || it == LANGUAGE_SPANISH }
            ?: LANGUAGE_VALENCIAN

        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(locale))
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}
