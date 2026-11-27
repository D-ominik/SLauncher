package de.juvento.plain.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.*
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import de.juvento.plain.BuildConfig
import de.juvento.plain.R
import de.juvento.plain.data.AppModel
import de.juvento.plain.data.appPrefKey
import de.juvento.plain.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*


fun showToastLong(context: Context, message: String) {
    val toast = Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

fun showToastShort(context: Context, message: String) {
    val toast = Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

/** Pinned shortcuts (e.g. Chrome web apps) of a profile; empty unless plain is the home app. */
private fun pinnedShortcuts(launcherApps: LauncherApps, profile: UserHandle): List<ShortcutInfo> {
    return try {
        if (!launcherApps.hasShortcutHostPermission()) return emptyList()
        val query = LauncherApps.ShortcutQuery().setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        launcherApps.getShortcuts(query, profile).orEmpty().filter { it.isEnabled }
    } catch (e: SecurityException) {
        emptyList()
    }
}

private fun ShortcutInfo.displayLabel(): String = (shortLabel ?: longLabel ?: id).toString()

suspend fun getAppsList(context: Context, showHiddenApps: Boolean = false): MutableList<AppModel> {
    return withContext(Dispatchers.IO) {
        val appList: MutableList<AppModel> = mutableListOf()

        try {
            val prefs = Prefs(context)
            val hiddenApps = prefs.hiddenApps
            val favorites = prefs.favoriteApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val collator = Collator.getInstance()

            fun add(label: String, appPackage: String, activity: String, profile: UserHandle, shortcutId: String?) {
                val prefKey = appPrefKey(appPackage, shortcutId)
                if (!showHiddenApps && hiddenApps.contains("$prefKey|$profile")) return
                appList.add(
                    AppModel(
                        label, collator.getCollationKey(label), appPackage, activity, profile,
                        prefs.getAppAlias(prefKey), prefs.getAppKeywords(prefKey), shortcutId,
                        favorites.contains("$prefKey|$profile"),
                    )
                )
            }

            for (profile in userManager.userProfiles) {
                for (app in launcherApps.getActivityList(null, profile)) {
                    val appPackage = app.applicationInfo.packageName
                    if (appPackage == BuildConfig.APPLICATION_ID) continue
                    add(app.label.toString(), appPackage, app.componentName.className, profile, null)
                }
                for (shortcut in pinnedShortcuts(launcherApps, profile)) {
                    if (shortcut.`package` == BuildConfig.APPLICATION_ID) continue
                    add(shortcut.displayLabel(), shortcut.`package`, "", profile, shortcut.id)
                }
            }

            appList.sortBy { it.name.lowercase() }
        } catch (e: Exception) {
            Log.e("AppList", "Could not load app list", e)
        }
        appList
    }
}

suspend fun getHiddenAppsList(context: Context): MutableList<AppModel> {
    val hidden = Prefs(context).hiddenApps
    return getAppsList(context, showHiddenApps = true).filterTo(mutableListOf()) { hidden.contains(it.identity) }
}

/** Removes a pinned shortcut from the launcher (the owning app is not affected). */
fun unpinShortcut(context: Context, app: AppModel) {
    val shortcutId = app.shortcutId ?: return
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    try {
        val remaining = pinnedShortcuts(launcherApps, app.user)
            .filter { it.`package` == app.appPackage && it.id != shortcutId }
            .map { it.id }
        launcherApps.pinShortcuts(app.appPackage, remaining, app.user)
    } catch (e: SecurityException) {
        Log.w("Shortcuts", "Cannot unpin shortcut", e)
    }
}

fun getUserHandleFromString(context: Context, userHandleString: String): UserHandle {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    for (userHandle in userManager.userProfiles) {
        if (userHandle.toString() == userHandleString) {
            return userHandle
        }
    }
    return android.os.Process.myUserHandle()
}

/** The label the system shows for the app or pinned shortcut, or null if it is gone. */
fun resolveAppLabel(context: Context, appPackage: String, userString: String, shortcutId: String = ""): String? {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val user = getUserHandleFromString(context, userString)
    if (shortcutId.isNotEmpty()) {
        return pinnedShortcuts(launcher, user).firstOrNull { it.`package` == appPackage && it.id == shortcutId }?.displayLabel()
    }
    return launcher.getActivityList(appPackage, user).firstOrNull()?.label?.toString()
}

fun isDefaultLauncher(context: Context): Boolean =
    BuildConfig.APPLICATION_ID == getDefaultLauncherPackage(context)

fun getDefaultLauncherPackage(context: Context): String {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val result = context.packageManager.resolveActivity(intent, 0)
    return result?.activityInfo?.packageName ?: "android"
}

/** System dialog "Set plain as home app?"; null if the role cannot be requested (already held or unavailable). */
fun createHomeRoleRequestIntent(context: Context): Intent? {
    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
    if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME) || roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return null
    return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
}

/** Opens the system page "Default home app", where any installed launcher can be chosen. */
fun openDefaultLauncherSettings(context: Context) {
    val candidates = listOf(
        Intent(Settings.ACTION_HOME_SETTINGS),
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
    )
    for (intent in candidates) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
    showToastLong(context, context.getString(R.string.search_launcher_in_settings))
}

fun openAppInfo(context: Context, userHandle: UserHandle, packageName: String) {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val component = launcher.getActivityList(packageName, userHandle).firstOrNull()?.componentName
    if (component == null) {
        showToastShort(context, context.getString(R.string.app_info_unavailable))
        return
    }
    launcher.startAppDetailsActivity(component, userHandle, null, null)
}

fun openDialerApp(context: Context) {
    try {
        val sendIntent = Intent(Intent.ACTION_DIAL)
        context.startActivity(sendIntent)
    } catch (e: java.lang.Exception) {

    }
}

fun openCameraApp(context: Context) {
    try {
        val sendIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        context.startActivity(sendIntent)
    } catch (e: java.lang.Exception) {

    }
}

fun openAlarmApp(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        context.startActivity(intent)
    } catch (e: java.lang.Exception) {
        Log.d("TAG", e.toString())
    }
}

fun openCalendar(context: Context) {
    try {
        val cal: Calendar = Calendar.getInstance()
        cal.time = Date()
        val time = cal.time.time
        val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        builder.appendPath(time.toString())
        context.startActivity(Intent(Intent.ACTION_VIEW, builder.build()))
    } catch (e: Exception) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            context.startActivity(intent)
        } catch (e: Exception) {
        }
    }
}

fun isTablet(context: Context): Boolean = context.resources.configuration.smallestScreenWidthDp >= 600

fun initActionService(context: Context): ActionService? {
    val actionService = ActionService.instance()
    if (actionService == null) openAccessibilitySettings(context)
    return actionService
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    val cs = ComponentName(context.packageName, ActionService::class.java.name).flattenToString()
    val bundle = Bundle()
    bundle.putString(":settings:fragment_args_key", cs)
    intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(":settings:fragment_args_key", cs)
        putExtra(":settings:show_fragment_args", bundle)
    }
    context.startActivity(intent)
}

fun showStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        activity.window.insetsController?.show(WindowInsets.Type.statusBars())
    else
        @Suppress("DEPRECATION", "InlinedApi")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
}

fun hideStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
    else {
        @Suppress("DEPRECATION")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}

fun uninstallApp(context: Context, appPackage: String) {
    val intent = Intent(Intent.ACTION_DELETE)
    intent.data = Uri.parse("package:$appPackage")
    context.startActivity(intent)
}

fun dp2px(resources: Resources, dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

@Suppress("SpellCheckingInspection")
@SuppressLint("WrongConstant")
fun expandNotificationDrawer(context: Context) {
    // Source: https://stackoverflow.com/a/51132142
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Suppress("SpellCheckingInspection")
@SuppressLint("WrongConstant")
fun expandQuickSettings(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandSettingsPanel")
        method.invoke(statusBarService)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
