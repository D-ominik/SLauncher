package de.juvento.plain

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import de.juvento.plain.data.Constants
import de.juvento.plain.data.PREF_ORIENTATION
import de.juvento.plain.data.Prefs
import de.juvento.plain.helper.createHomeRoleRequestIntent
import de.juvento.plain.helper.openDefaultLauncherSettings
import de.juvento.plain.helper.showToastLong

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    // The system file picker would otherwise count as "leaving the launcher" and pop the settings.
    private var keepScreenOnNextLeave = false

    private lateinit var prefs: Prefs

    // SharedPreferences only holds weak references, so the listener must be a field.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PREF_ORIENTATION) applyOrientation()
    }

    private val homeRoleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        keepScreenOnNextLeave = false
    }

    private val exportBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(::writeBackup) }

    private val importBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(::readBackup) }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        val themeMode = when (prefs.appTheme) {
            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)

        super.onCreate(savedInstanceState)
        // Draw behind the (fully transparent) system bars; screens handle the insets themselves.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        // The home screen is the root of a launcher and must never be closed by "back".
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id != R.id.mainFragment) navController.popBackStack()
            }
        })

        applyOrientation()
        prefs.registerChangeListener(prefsListener)
    }

    override fun onDestroy() {
        prefs.unregisterChangeListener(prefsListener)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        keepScreenOnNextLeave = false
    }

    override fun onStop() {
        // A rotation recreates the activity; that must not throw the user back to the home screen.
        if (!isChangingConfigurations) backToHomeScreen()
        super.onStop()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        keepScreenOnNextLeave = false
        backToHomeScreen()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    private fun applyOrientation() {
        requestedOrientation = prefs.orientation.requested
    }

    private fun backToHomeScreen() {
        if (keepScreenOnNextLeave) return
        // Pop everything except the home screen whenever the user leaves the launcher.
        if (navController.currentDestination?.id != R.id.mainFragment)
            navController.popBackStack(R.id.mainFragment, false)
    }

    /** Asks the system to make plain the home app; falls back to the system settings page. */
    fun requestDefaultLauncher() {
        val intent = createHomeRoleRequestIntent(this)
        if (intent == null) {
            openDefaultLauncherSettings(this)
            return
        }
        keepScreenOnNextLeave = true
        try {
            homeRoleRequest.launch(intent)
        } catch (e: ActivityNotFoundException) {
            keepScreenOnNextLeave = false
            openDefaultLauncherSettings(this)
        }
    }

    fun startBackupExport() {
        keepScreenOnNextLeave = true
        exportBackup.launch("plain-backup.json")
    }

    fun startBackupImport() {
        keepScreenOnNextLeave = true
        importBackup.launch(arrayOf("*/*"))
    }

    private fun writeBackup(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(Prefs(this).saveToString().toByteArray())
            } ?: throw IllegalStateException("no stream")
            showToastLong(this, getString(R.string.backup_saved))
        } catch (e: Exception) {
            showToastLong(this, getString(R.string.backup_failed))
        }
    }

    private fun readBackup(uri: Uri) {
        val text = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        } catch (e: Exception) {
            null
        }
        if (text == null || !Prefs(this).loadFromString(text)) {
            showToastLong(this, getString(R.string.backup_invalid))
            return
        }
        showToastLong(this, getString(R.string.backup_restored))
        recreate()
    }
}
