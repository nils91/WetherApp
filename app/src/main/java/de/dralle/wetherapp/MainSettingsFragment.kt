package de.dralle.wetherapp

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat

class MainSettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        //inflate
       setPreferencesFromResource(R.xml.application_settings_main,rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Deactivate settings menu button
        if(activity is WetherAppMainActivity){
            (activity as WetherAppMainActivity).deactivateSettingsMenu()
        }
    }
}