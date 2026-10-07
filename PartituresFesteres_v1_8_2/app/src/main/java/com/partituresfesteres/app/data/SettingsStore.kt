package com.partituresfesteres.app.data

import android.content.Context

data class AppSettings(
    val menuHideSeconds: Int = 3,
    val showScoreNameOnChange: Boolean = false,
    val rememberZoom: Boolean = true,
    val keepScreenOn: Boolean = true,
    val allowViewerRotation: Boolean = true,
    val sideTapFraction: Float = 0.22f,
    val overrideBrightness: Boolean = false,
    val brightnessPercent: Int = 80,
    val tunerReferenceHz: Int = 442,
    val appLanguage: String = LanguageManager.LANGUAGE_VALENCIAN,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        menuHideSeconds = prefs.getInt(KEY_MENU_HIDE_SECONDS, 3).coerceIn(1, 10),
        showScoreNameOnChange = prefs.getBoolean(KEY_SHOW_SCORE_NAME, false),
        rememberZoom = prefs.getBoolean(KEY_REMEMBER_ZOOM, true),
        keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
        allowViewerRotation = prefs.getBoolean(KEY_ALLOW_ROTATION, true),
        sideTapFraction = prefs.getFloat(KEY_SIDE_TAP_FRACTION, 0.22f).coerceIn(0.12f, 0.32f),
        overrideBrightness = prefs.getBoolean(KEY_OVERRIDE_BRIGHTNESS, false),
        brightnessPercent = prefs.getInt(KEY_BRIGHTNESS_PERCENT, 80).coerceIn(20, 100),
        tunerReferenceHz = prefs.getInt(KEY_TUNER_REFERENCE_HZ, 442).coerceIn(430, 450),
        appLanguage = prefs.getString(KEY_APP_LANGUAGE, LanguageManager.LANGUAGE_VALENCIAN)
            ?.takeIf { it == LanguageManager.LANGUAGE_VALENCIAN || it == LanguageManager.LANGUAGE_SPANISH }
            ?: LanguageManager.LANGUAGE_VALENCIAN,
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putInt(KEY_MENU_HIDE_SECONDS, settings.menuHideSeconds.coerceIn(1, 10))
            .putBoolean(KEY_SHOW_SCORE_NAME, settings.showScoreNameOnChange)
            .putBoolean(KEY_REMEMBER_ZOOM, settings.rememberZoom)
            .putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)
            .putBoolean(KEY_ALLOW_ROTATION, settings.allowViewerRotation)
            .putFloat(KEY_SIDE_TAP_FRACTION, settings.sideTapFraction.coerceIn(0.12f, 0.32f))
            .putBoolean(KEY_OVERRIDE_BRIGHTNESS, settings.overrideBrightness)
            .putInt(KEY_BRIGHTNESS_PERCENT, settings.brightnessPercent.coerceIn(20, 100))
            .putInt(KEY_TUNER_REFERENCE_HZ, settings.tunerReferenceHz.coerceIn(430, 450))
            .putString(KEY_APP_LANGUAGE, settings.appLanguage)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "partitures_festeres_settings"
        private const val KEY_MENU_HIDE_SECONDS = "menu_hide_seconds"
        private const val KEY_SHOW_SCORE_NAME = "show_score_name"
        private const val KEY_REMEMBER_ZOOM = "remember_zoom"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_ALLOW_ROTATION = "allow_viewer_rotation"
        private const val KEY_SIDE_TAP_FRACTION = "side_tap_fraction"
        private const val KEY_OVERRIDE_BRIGHTNESS = "override_brightness"
        private const val KEY_BRIGHTNESS_PERCENT = "brightness_percent"
        private const val KEY_TUNER_REFERENCE_HZ = "tuner_reference_hz"
        const val KEY_APP_LANGUAGE = "app_language"
    }
}
