package de.juvento.plain.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import de.juvento.plain.R
import de.juvento.plain.data.AppModel
import de.juvento.plain.data.Constants.Action
import de.juvento.plain.data.Prefs

@Composable
fun GesturesPage(prefs: Prefs, onBack: () -> Unit) {
    var swipeLeft by remember { mutableStateOf(prefs.swipeLeftAction) }
    var swipeRight by remember { mutableStateOf(prefs.swipeRightAction) }
    var swipeUp by remember { mutableStateOf(prefs.swipeUpAction) }
    var swipeDown by remember { mutableStateOf(prefs.swipeDownAction) }
    var clock by remember { mutableStateOf(prefs.clickClockAction) }
    var date by remember { mutableStateOf(prefs.clickDateAction) }
    var doubleTap by remember { mutableStateOf(prefs.doubleTapAction) }

    var swipeLeftApp by remember { mutableStateOf(prefs.appSwipeLeft) }
    var swipeRightApp by remember { mutableStateOf(prefs.appSwipeRight) }
    var swipeUpApp by remember { mutableStateOf(prefs.appSwipeUp) }
    var swipeDownApp by remember { mutableStateOf(prefs.appSwipeDown) }
    var clockApp by remember { mutableStateOf(prefs.appClickClock) }
    var dateApp by remember { mutableStateOf(prefs.appClickDate) }
    var doubleTapApp by remember { mutableStateOf(prefs.appDoubleTap) }

    SettingsPage(title = stringResource(R.string.gestures), onBack = onBack) {
        GestureSetting(
            title = stringResource(R.string.swipe_left_app),
            defaultApp = stringResource(R.string.default_app_camera),
            action = swipeLeft, app = swipeLeftApp,
            onAction = { swipeLeft = it; prefs.swipeLeftAction = it },
            onApp = { swipeLeftApp = it; prefs.appSwipeLeft = it },
        )
        GestureSetting(
            title = stringResource(R.string.swipe_right_app),
            defaultApp = stringResource(R.string.default_app_phone),
            action = swipeRight, app = swipeRightApp,
            onAction = { swipeRight = it; prefs.swipeRightAction = it },
            onApp = { swipeRightApp = it; prefs.appSwipeRight = it },
        )
        GestureSetting(
            title = stringResource(R.string.swipe_up_app),
            defaultApp = stringResource(R.string.show_app_list),
            action = swipeUp, app = swipeUpApp,
            onAction = { swipeUp = it; prefs.swipeUpAction = it },
            onApp = { swipeUpApp = it; prefs.appSwipeUp = it },
        )
        GestureSetting(
            title = stringResource(R.string.swipe_down_app),
            defaultApp = stringResource(R.string.default_app_phone),
            action = swipeDown, app = swipeDownApp,
            onAction = { swipeDown = it; prefs.swipeDownAction = it },
            onApp = { swipeDownApp = it; prefs.appSwipeDown = it },
        )
        GestureSetting(
            title = stringResource(R.string.clock_click_app),
            defaultApp = stringResource(R.string.default_app_clock),
            action = clock, app = clockApp,
            onAction = { clock = it; prefs.clickClockAction = it },
            onApp = { clockApp = it; prefs.appClickClock = it },
        )
        GestureSetting(
            title = stringResource(R.string.date_click_app),
            defaultApp = stringResource(R.string.default_app_calendar),
            action = date, app = dateApp,
            onAction = { date = it; prefs.clickDateAction = it },
            onApp = { dateApp = it; prefs.appClickDate = it },
        )
        GestureSetting(
            title = stringResource(R.string.double_tap),
            defaultApp = stringResource(R.string.default_app_camera),
            action = doubleTap, app = doubleTapApp,
            onAction = { doubleTap = it; prefs.doubleTapAction = it },
            onApp = { doubleTapApp = it; prefs.appDoubleTap = it },
            last = true,
        )
    }
}

/** Action dropdown; for "Open app" an additional row shows and changes the target app. */
@Composable
private fun GestureSetting(
    title: String,
    defaultApp: String,
    action: Action,
    app: AppModel,
    onAction: (Action) -> Unit,
    onApp: (AppModel) -> Unit,
    last: Boolean = false,
) {
    var picking by remember { mutableStateOf(false) }

    DropdownRow(title, action, Action.values().toList(), { it.string() }) {
        onAction(it)
        // choosing "Open app" immediately asks for the app, as before
        if (it == Action.OpenApp && app.appPackage.isEmpty()) picking = true
    }
    if (action == Action.OpenApp) {
        SettingsRow(
            title = stringResource(R.string.gesture_app),
            subtitle = app.appLabel.ifEmpty { stringResource(R.string.default_app_hint, defaultApp) },
            onClick = { picking = true },
        )
    }
    if (!last) SettingsDivider()

    if (picking) {
        AppPickerDialog(
            title = title,
            multi = false,
            onDismiss = { picking = false },
            onConfirm = { picked ->
                onApp(picked.first())
                picking = false
            },
        )
    }
}
