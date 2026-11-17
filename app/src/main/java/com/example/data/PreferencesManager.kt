package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.SupportedLanguages
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meldin_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
    }

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemePreference.SYSTEM.name
        ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.SYSTEM
    }

    val languageCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LANGUAGE_CODE] ?: defaultDeviceLanguageCode()
    }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = preference.name
        }
    }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = code
        }
    }

    private fun defaultDeviceLanguageCode(): String {
        val deviceLang = Locale.getDefault().language
        return if (SupportedLanguages.any { it.code == deviceLang }) deviceLang else "en"
    }
}
