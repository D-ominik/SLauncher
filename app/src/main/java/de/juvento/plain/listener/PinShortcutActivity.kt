package de.juvento.plain.listener

import android.app.Activity
import android.content.pm.LauncherApps
import android.os.Bundle
import de.juvento.plain.R
import de.juvento.plain.helper.showToastLong

/**
 * Target of `CONFIRM_PIN_SHORTCUT`: apps such as Chrome ask the home app to pin a shortcut
 * (e.g. a web app). Pinned shortcuts show up in the app drawer and can be used in app sets.
 */
class PinShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = launcherApps.getPinItemRequest(intent)
        val accepted = request != null &&
            request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT &&
            request.isValid && request.accept()
        val label = request?.shortcutInfo?.shortLabel
        showToastLong(
            this,
            if (accepted) getString(R.string.shortcut_added, label.toString()) else getString(R.string.shortcut_not_added)
        )
        finish()
    }
}
