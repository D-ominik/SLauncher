package de.juvento.plain.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import de.juvento.plain.R
import de.juvento.plain.helper.getUserHandleFromString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val PREFS_FILENAME = "de.juvento.plain"

private const val FIRST_OPEN = "FIRST_OPEN"
private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
private const val AUTO_SHOW_KEYBOARD = "AUTO_SHOW_KEYBOARD"
private const val AUTO_OPEN_APP = "AUTO_OPEN_APP"
private const val HOME_ALIGNMENT = "HOME_ALIGNMENT"
private const val HOME_ALIGNMENT_BOTTOM = "HOME_ALIGNMENT_BOTTOM"
private const val HOME_CLICK_AREA = "HOME_CLICK_AREA"
private const val DRAWER_ALIGNMENT = "DRAWER_ALIGNMENT"
private const val TIME_ALIGNMENT = "TIME_ALIGNMENT"
private const val STATUS_BAR = "STATUS_BAR"
private const val SHOW_DATE = "SHOW_DATE"
private const val SHOW_TIME = "SHOW_TIME"
private const val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
private const val SWIPE_UP_ACTION = "SWIPE_UP_ACTION"
private const val SWIPE_RIGHT_ACTION = "SWIPE_RIGHT_ACTION"
private const val SWIPE_LEFT_ACTION = "SWIPE_LEFT_ACTION"
private const val CLICK_CLOCK_ACTION = "CLICK_CLOCK_ACTION"
private const val CLICK_DATE_ACTION = "CLICK_DATE_ACTION"
private const val DOUBLE_TAP_ACTION = "DOUBLE_TAP_ACTION"
private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val FAVORITE_APPS = "FAVORITE_APPS"
private const val APP_THEME = "APP_THEME"
private const val TEXT_SIZE = "text_size"
private const val APP_FONT = "APP_FONT"
const val PREF_ORIENTATION = "ORIENTATION"
private const val TEXT_COLOR_LIGHT = "TEXT_COLOR_LIGHT"
private const val TEXT_COLOR_DARK = "TEXT_COLOR_DARK"

const val PREF_APP_SETS = "APP_SETS"
const val PREF_ACTIVE_SET = "ACTIVE_SET"
private const val ALLOW_INTENT_CONTROL = "ALLOW_INTENT_CONTROL"

private const val APP_NAME = "APP_NAME"
private const val APP_PACKAGE = "APP_PACKAGE"
private const val APP_USER = "APP_USER"
private const val APP_ALIAS = "APP_ALIAS"
private const val APP_ACTIVITY = "APP_ACTIVITY"
private const val APP_SHORTCUT = "APP_SHORTCUT"
private const val ALIAS_PREFIX = "ALIAS_"
private const val KEYWORDS_PREFIX = "KEYWORDS_"

private const val SWIPE_RIGHT = "SWIPE_RIGHT"
private const val SWIPE_LEFT = "SWIPE_LEFT"
private const val SWIPE_DOWN = "SWIPE_DOWN"
private const val SWIPE_UP = "SWIPE_UP"
private const val CLICK_CLOCK = "CLICK_CLOCK"
private const val CLICK_DATE = "CLICK_DATE"
private const val DOUBLE_TAP = "DOUBLE_TAP"

object Keywords {
    /** Keywords are separated by commas or line breaks. */
    fun parse(raw: String): List<String> =
        raw.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

    fun format(keywords: List<String>): String = keywords.joinToString("\n")
}

class Prefs(val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    fun saveToString(): String {
        val json = JSONObject()
        for ((key, value) in prefs.all) {
            when (value) {
                is Set<*> -> json.put(key, JSONArray(value.filterIsInstance<String>()))
                null -> {}
                else -> json.put(key, value)
            }
        }
        return json.toString()
    }

    /**
     * Replaces all settings with the backup. The backup is parsed completely first, so a broken
     * file leaves the current settings untouched.
     * @return false if [json] is not a valid backup
     */
    fun loadFromString(json: String): Boolean {
        val all = try {
            JSONObject(json)
        } catch (_: JSONException) {
            return false
        }
        val editor = prefs.edit().clear()
        for (key in all.keys()) {
            when (val value = all.get(key)) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putInt(key, value.toInt())
                is Double -> editor.putInt(key, value.toInt())
                is JSONArray -> editor.putStringSet(key, (0 until value.length()).map { value.getString(it) }.toSet())
            }
        }
        editor.apply()
        return true
    }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    // true on the first read, false afterwards
    fun firstOpen(): Boolean = firstTrueFalseAfter(FIRST_OPEN)

    fun firstSettingsOpen(): Boolean = firstTrueFalseAfter(FIRST_SETTINGS_OPEN)

    var autoShowKeyboard: Boolean
        get() = prefs.getBoolean(AUTO_SHOW_KEYBOARD, true)
        set(value) = prefs.edit().putBoolean(AUTO_SHOW_KEYBOARD, value).apply()

    var autoOpenApp: Boolean
        get() = prefs.getBoolean(AUTO_OPEN_APP, true)
        set(value) = prefs.edit().putBoolean(AUTO_OPEN_APP, value).apply()

    var homeAlignment: Constants.Gravity
        get() = enumPref(HOME_ALIGNMENT, Constants.Gravity.Left)
        set(value) = prefs.edit().putString(HOME_ALIGNMENT, value.name).apply()

    var homeAlignmentBottom: Boolean
        get() = prefs.getBoolean(HOME_ALIGNMENT_BOTTOM, false)
        set(value) = prefs.edit().putBoolean(HOME_ALIGNMENT_BOTTOM, value).apply()

    var extendHomeAppsArea: Boolean
        get() = prefs.getBoolean(HOME_CLICK_AREA, false)
        set(value) = prefs.edit().putBoolean(HOME_CLICK_AREA, value).apply()

    var clockAlignment: Constants.Gravity
        get() = enumPref(TIME_ALIGNMENT, Constants.Gravity.Left)
        set(value) = prefs.edit().putString(TIME_ALIGNMENT, value.name).apply()

    var drawerAlignment: Constants.Gravity
        get() = enumPref(DRAWER_ALIGNMENT, Constants.Gravity.Left)
        set(value) = prefs.edit().putString(DRAWER_ALIGNMENT, value.name).apply()

    var showStatusBar: Boolean
        get() = prefs.getBoolean(STATUS_BAR, true)
        set(value) = prefs.edit().putBoolean(STATUS_BAR, value).apply()

    var showTime: Boolean
        get() = prefs.getBoolean(SHOW_TIME, true)
        set(value) = prefs.edit().putBoolean(SHOW_TIME, value).apply()

    var showDate: Boolean
        get() = prefs.getBoolean(SHOW_DATE, true)
        set(value) = prefs.edit().putBoolean(SHOW_DATE, value).apply()

    var swipeLeftAction: Constants.Action
        get() = enumPref(SWIPE_LEFT_ACTION, Constants.Action.OpenApp)
        set(value) = prefs.edit().putString(SWIPE_LEFT_ACTION, value.name).apply()

    var swipeRightAction: Constants.Action
        get() = enumPref(SWIPE_RIGHT_ACTION, Constants.Action.OpenApp)
        set(value) = prefs.edit().putString(SWIPE_RIGHT_ACTION, value.name).apply()

    var swipeDownAction: Constants.Action
        get() = enumPref(SWIPE_DOWN_ACTION, Constants.Action.ShowNotification)
        set(value) = prefs.edit().putString(SWIPE_DOWN_ACTION, value.name).apply()

    var swipeUpAction: Constants.Action
        get() = enumPref(SWIPE_UP_ACTION, Constants.Action.ShowAppList)
        set(value) = prefs.edit().putString(SWIPE_UP_ACTION, value.name).apply()

    var clickClockAction: Constants.Action
        get() = enumPref(CLICK_CLOCK_ACTION, Constants.Action.OpenApp)
        set(value) = prefs.edit().putString(CLICK_CLOCK_ACTION, value.name).apply()

    var clickDateAction: Constants.Action
        get() = enumPref(CLICK_DATE_ACTION, Constants.Action.OpenApp)
        set(value) = prefs.edit().putString(CLICK_DATE_ACTION, value.name).apply()

    var doubleTapAction: Constants.Action
        get() = enumPref(DOUBLE_TAP_ACTION, Constants.Action.LockScreen)
        set(value) = prefs.edit().putString(DOUBLE_TAP_ACTION, value.name).apply()

    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T =
        try {
            enumValueOf<T>(prefs.getString(key, default.name) ?: default.name)
        } catch (_: Exception) {
            default
        }

    var appTheme: Constants.Theme
        get() = enumPref(APP_THEME, Constants.Theme.System)
        set(value) = prefs.edit().putString(APP_THEME, value.name).apply()

    var orientation: Constants.Orientation
        get() = enumPref(PREF_ORIENTATION, Constants.Orientation.Auto)
        set(value) = prefs.edit().putString(PREF_ORIENTATION, value.name).apply()

    var font: Constants.AppFont
        get() = enumPref(APP_FONT, Constants.AppFont.SansLight)
        set(value) = prefs.edit().putString(APP_FONT, value.name).apply()

    var textSize: Int
        get() = try {
            prefs.getInt(TEXT_SIZE, Constants.TEXT_SIZE_DEFAULT)
        } catch (_: Exception) {
            Constants.TEXT_SIZE_DEFAULT
        }
        set(value) = prefs.edit().putInt(TEXT_SIZE, value).apply()

    /** ARGB text color for the light theme, or null for "automatic" (black). */
    var textColorLight: Int?
        get() = colorPref(TEXT_COLOR_LIGHT)
        set(value) = setColorPref(TEXT_COLOR_LIGHT, value)

    /** ARGB text color for the dark theme, or null for "automatic" (white). */
    var textColorDark: Int?
        get() = colorPref(TEXT_COLOR_DARK)
        set(value) = setColorPref(TEXT_COLOR_DARK, value)

    private fun colorPref(key: String): Int? =
        try {
            if (prefs.contains(key)) prefs.getInt(key, 0) else null
        } catch (_: Exception) {
            null
        }

    private fun setColorPref(key: String, value: Int?) {
        val editor = prefs.edit()
        if (value == null) editor.remove(key) else editor.putInt(key, value)
        editor.apply()
    }

    var hiddenApps: Set<String>
        get() = HashSet(prefs.getStringSet(HIDDEN_APPS, emptySet()) ?: emptySet())
        set(value) = prefs.edit().putStringSet(HIDDEN_APPS, value).apply()

    /** [AppModel.identity] of every favorite app. */
    var favoriteApps: Set<String>
        get() = HashSet(prefs.getStringSet(FAVORITE_APPS, emptySet()) ?: emptySet())
        set(value) = prefs.edit().putStringSet(FAVORITE_APPS, value).apply()

    // ---- app sets -------------------------------------------------------------------------

    /** All sets; never empty (a default set is provided if nothing has been stored yet). */
    var appSets: List<AppSet>
        get() = AppSets.decode(prefs.getString(PREF_APP_SETS, null)).ifEmpty {
            listOf(AppSet(AppSets.DEFAULT_SET_ID, context.getString(R.string.default_set_name), emptyList()))
        }
        set(value) = prefs.edit().putString(PREF_APP_SETS, AppSets.encode(value)).apply()

    var activeSetId: String
        get() = prefs.getString(PREF_ACTIVE_SET, null) ?: AppSets.DEFAULT_SET_ID
        set(value) = prefs.edit().putString(PREF_ACTIVE_SET, value).apply()

    val activeSet: AppSet
        get() {
            val sets = appSets
            return sets.firstOrNull { it.id == activeSetId } ?: sets.first()
        }

    /** Whether other apps may switch the active set via Intent. */
    var allowIntentControl: Boolean
        get() = prefs.getBoolean(ALLOW_INTENT_CONTROL, true)
        set(value) = prefs.edit().putBoolean(ALLOW_INTENT_CONTROL, value).apply()

    /** @return false if no set with that name exists */
    fun activateSetByName(name: String): Boolean {
        val set = AppSets.findByName(appSets, name) ?: return false
        activeSetId = set.id
        return true
    }

    // ---- gesture targets ------------------------------------------------------------------

    var appSwipeRight: AppModel
        get() = loadApp(SWIPE_RIGHT)
        set(appModel) = storeApp(SWIPE_RIGHT, appModel)

    var appSwipeLeft: AppModel
        get() = loadApp(SWIPE_LEFT)
        set(appModel) = storeApp(SWIPE_LEFT, appModel)

    var appSwipeDown: AppModel
        get() = loadApp(SWIPE_DOWN)
        set(appModel) = storeApp(SWIPE_DOWN, appModel)

    var appSwipeUp: AppModel
        get() = loadApp(SWIPE_UP)
        set(appModel) = storeApp(SWIPE_UP, appModel)

    var appClickClock: AppModel
        get() = loadApp(CLICK_CLOCK)
        set(appModel) = storeApp(CLICK_CLOCK, appModel)

    var appClickDate: AppModel
        get() = loadApp(CLICK_DATE)
        set(appModel) = storeApp(CLICK_DATE, appModel)

    var appDoubleTap: AppModel
        get() = loadApp(DOUBLE_TAP)
        set(appModel) = storeApp(DOUBLE_TAP, appModel)

    private fun loadApp(id: String): AppModel {
        val name = prefs.getString("${APP_NAME}_$id", "").orEmpty()
        val pack = prefs.getString("${APP_PACKAGE}_$id", "").orEmpty()
        val alias = prefs.getString("${APP_ALIAS}_$id", "").orEmpty()
        val activity = prefs.getString("${APP_ACTIVITY}_$id", "").orEmpty()
        val userHandle: UserHandle = getUserHandleFromString(context, prefs.getString("${APP_USER}_$id", "").orEmpty())

        return AppModel(
            appLabel = name,
            appPackage = pack,
            appAlias = alias,
            appActivityName = activity,
            user = userHandle,
            key = null,
            shortcutId = prefs.getString("${APP_SHORTCUT}_$id", "").orEmpty().ifEmpty { null },
        )
    }

    private fun storeApp(id: String, appModel: AppModel) {
        prefs.edit()
            .putString("${APP_NAME}_$id", appModel.appLabel)
            .putString("${APP_PACKAGE}_$id", appModel.appPackage)
            .putString("${APP_ACTIVITY}_$id", appModel.appActivityName)
            .putString("${APP_ALIAS}_$id", appModel.appAlias)
            .putString("${APP_USER}_$id", appModel.user.toString())
            .putString("${APP_SHORTCUT}_$id", appModel.shortcutId.orEmpty())
            .apply()
    }

    // ---- app drawer names and keywords ----------------------------------------------------

    fun getAppAlias(appPackage: String): String = prefs.getString(ALIAS_PREFIX + appPackage, "").orEmpty()

    fun getAppKeywords(appPackage: String): List<String> =
        Keywords.parse(prefs.getString(KEYWORDS_PREFIX + appPackage, "").orEmpty())

    fun setAppAliasAndKeywords(appPackage: String, alias: String, keywords: List<String>) {
        val editor = prefs.edit()
        if (alias.isEmpty()) editor.remove(ALIAS_PREFIX + appPackage) else editor.putString(ALIAS_PREFIX + appPackage, alias)
        if (keywords.isEmpty()) editor.remove(KEYWORDS_PREFIX + appPackage)
        else editor.putString(KEYWORDS_PREFIX + appPackage, Keywords.format(keywords))
        editor.apply()
    }

    private fun firstTrueFalseAfter(key: String): Boolean {
        val first = prefs.getBoolean(key, true)
        if (first) prefs.edit().putBoolean(key, false).apply()
        return first
    }
}
