package com.martinez.simulago.ui.settigns

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.martinez.simulago.BuildConfig

import com.martinez.simulago.R


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        // Lógica para el cambio de tema
        val themePreference: ListPreference? = findPreference("theme")
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue as String) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true // Indica que el cambio fue manejado
        }

        // Lógica para el botón de compartir
        val sharePreference: Preference? = findPreference("share")
        sharePreference?.setOnPreferenceClickListener {
            shareApp()
            true // Indica que el clic fue manejado
        }

        // Lógica para mostrar la versión (opcional, podrías obtenerla del BuildConfig)
        val versionPreference: Preference? = findPreference("version")
         versionPreference?.summary = BuildConfig.VERSION_NAME // Forma avanzada
    }

    private fun shareApp() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "¡Hola! Te recomiendo esta increíble calculadora de créditos: [Enlace a Google Play]")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir SimulaGo")
        startActivity(shareIntent)
    }
}