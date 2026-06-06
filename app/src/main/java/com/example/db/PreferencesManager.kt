package com.example.db

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hz_chord_ai_preferences")

class PreferencesManager(private val context: Context) {
    companion object {
        val USE_FLAT_NAMING = booleanPreferencesKey("use_flat_naming")
        val VISUAL_LAYOUT_MODE = stringPreferencesKey("visual_layout_mode")
        val TUNING_CHOICE = stringPreferencesKey("tuning_choice")
    }

    val useFlatNamingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_FLAT_NAMING] ?: false
    }

    val visualLayoutModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[VISUAL_LAYOUT_MODE] ?: "MATRIX"
    }

    val tuningChoiceFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TUNING_CHOICE] ?: "Standard A=440Hz"
    }

    suspend fun setUseFlatNaming(useFlat: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_FLAT_NAMING] = useFlat
        }
    }

    suspend fun setVisualLayoutMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[VISUAL_LAYOUT_MODE] = mode
        }
    }

    suspend fun setTuningChoice(tuning: String) {
        context.dataStore.edit { preferences ->
            preferences[TUNING_CHOICE] = tuning
        }
    }
}
