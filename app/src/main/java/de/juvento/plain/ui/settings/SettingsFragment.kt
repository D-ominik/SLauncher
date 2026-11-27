package de.juvento.plain.ui.settings

import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import de.juvento.plain.BuildConfig
import de.juvento.plain.MainActivity
import de.juvento.plain.MainViewModel
import de.juvento.plain.R
import de.juvento.plain.data.Constants.AppDrawerFlag
import de.juvento.plain.helper.openDefaultLauncherSettings
import de.juvento.plain.helper.openAppInfo

class SettingsFragment : Fragment(), SettingsHost {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SettingsTheme {
                    SettingsScreen(host = this@SettingsFragment)
                }
            }
        }

    override fun close() {
        findNavController().popBackStack()
    }

    override fun openHiddenApps() {
        ViewModelProvider(requireActivity())[MainViewModel::class.java].getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf("flag" to AppDrawerFlag.HiddenApps.name),
        )
    }

    override fun openOwnAppInfo() = openAppInfo(requireContext(), Process.myUserHandle(), BuildConfig.APPLICATION_ID)

    override fun chooseDefaultLauncher() = openDefaultLauncherSettings(requireContext())

    override fun exportBackup() = (requireActivity() as MainActivity).startBackupExport()

    override fun importBackup() = (requireActivity() as MainActivity).startBackupImport()
}
