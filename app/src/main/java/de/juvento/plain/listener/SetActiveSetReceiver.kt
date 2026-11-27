package de.juvento.plain.listener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.juvento.plain.data.Constants
import de.juvento.plain.data.Prefs

/**
 * Switches the active home app set, e.g. from Tasker ("Send Intent", target "Broadcast"):
 *
 *  - Action:  `de.juvento.plain.action.SET_ACTIVE_SET`
 *  - Package: `de.juvento.plain`
 *  - Extra:   `set:<name of the app set>`
 */
class SetActiveSetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.ACTION_SET_ACTIVE_SET) return
        val prefs = Prefs(context)
        if (!prefs.allowIntentControl) {
            Log.w(TAG, "Ignoring intent: external control is disabled in the settings")
            return
        }
        val name = intent.getStringExtra(Constants.EXTRA_SET_NAME)
        if (name.isNullOrBlank() || !prefs.activateSetByName(name)) {
            Log.w(TAG, "Ignoring intent: no app set named '$name'")
        }
    }

    private companion object {
        const val TAG = "SetActiveSetReceiver"
    }
}
