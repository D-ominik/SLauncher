package de.juvento.plain.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/** One entry of a home screen app set. [label] is independent of the app drawer name. */
data class HomeApp(
    val appPackage: String,
    val activityName: String,
    /** `UserHandle.toString()` of the profile the app lives in. */
    val user: String,
    val label: String,
    /** Empty for normal apps, otherwise the id of a pinned shortcut of [appPackage]. */
    val shortcutId: String = "",
) {
    val identity: String get() = "${appPrefKey(appPackage, shortcutId.ifEmpty { null })}|$user"
}

data class AppSet(
    val id: String,
    val name: String,
    val apps: List<HomeApp>,
)

enum class SetNameError { Empty, Duplicate }

object AppSets {

    const val NAME_MAX_LENGTH = 30
    const val DEFAULT_SET_ID = "default"

    /**
     * Set names are addressed from outside (Intents, e.g. Tasker), so they are limited to
     * letters, digits, space, '.', '_' and '-'.
     */
    fun isAllowedChar(c: Char): Boolean = c.isLetterOrDigit() || c == ' ' || c == '.' || c == '_' || c == '-'

    fun sanitizeInput(raw: String): String = raw.filter(::isAllowedChar).take(NAME_MAX_LENGTH)

    fun normalizeName(raw: String): String = sanitizeInput(raw).trim()

    fun validateName(raw: String, existing: List<AppSet>, ownId: String?): SetNameError? {
        val name = normalizeName(raw)
        if (name.isEmpty()) return SetNameError.Empty
        val clash = existing.any { it.id != ownId && it.name.equals(name, ignoreCase = true) }
        return if (clash) SetNameError.Duplicate else null
    }

    fun findByName(sets: List<AppSet>, name: String): AppSet? {
        val wanted = name.trim()
        return sets.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun encode(sets: List<AppSet>): String {
        val array = JSONArray()
        for (set in sets) {
            val apps = JSONArray()
            for (app in set.apps) {
                apps.put(
                    JSONObject()
                        .put("package", app.appPackage)
                        .put("activity", app.activityName)
                        .put("user", app.user)
                        .put("label", app.label)
                        .put("shortcut", app.shortcutId)
                )
            }
            array.put(JSONObject().put("id", set.id).put("name", set.name).put("apps", apps))
        }
        return array.toString()
    }

    /** Returns an empty list for missing or corrupt data instead of throwing. */
    fun decode(json: String?): List<AppSet> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val apps = obj.getJSONArray("apps")
                AppSet(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    apps = (0 until apps.length()).map { j ->
                        val app = apps.getJSONObject(j)
                        HomeApp(
                            appPackage = app.getString("package"),
                            activityName = app.optString("activity"),
                            user = app.optString("user"),
                            label = app.optString("label"),
                            shortcutId = app.optString("shortcut"),
                        )
                    }
                )
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }
}
