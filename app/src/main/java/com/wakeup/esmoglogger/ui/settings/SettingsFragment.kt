package com.wakeup.esmoglogger.ui.settings

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.wakeup.esmoglogger.PREFS_DARKMODE
import com.wakeup.esmoglogger.PREFS_KEY
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.databinding.FragmentSettingsBinding
import androidx.core.content.edit


class SettingsFragment : Fragment()  {
    private var binding: FragmentSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding!!.getRoot()

        val darkThemeEnabled: SwitchMaterial = root.findViewById(R.id.dark_theme_enabled)

        darkThemeEnabled.setOnCheckedChangeListener { _, isChecked ->
            requireContext().getSharedPreferences(PREFS_KEY, MODE_PRIVATE).edit {
                putBoolean(PREFS_DARKMODE, isChecked)
            }
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}