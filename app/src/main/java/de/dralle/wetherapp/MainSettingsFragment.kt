package de.dralle.wetherapp

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class MainSettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
       setPreferencesFromResource(R.xml.application_settings_main,rootKey)
    }
}