package de.juvento.plain.data

import android.os.UserHandle
import java.text.CollationKey

data class AppModel(
    val appLabel: String,
    val key: CollationKey?,
    val appPackage: String,
    val appActivityName: String,
    val user: UserHandle,
    var appAlias: String,
    /** Extra search terms, one per entry. */
    var keywords: List<String> = emptyList(),
    /** Set for pinned shortcuts (e.g. Chrome web apps); [appPackage] is then the package that owns it. */
    val shortcutId: String? = null,
    var isFavorite: Boolean = false,
) : Comparable<AppModel> {
    override fun compareTo(other: AppModel): Int = when {
        key != null && other.key != null -> key.compareTo(other.key)
        else -> appLabel.compareTo(other.appLabel, true)
    }

    val name: String get() = appAlias.ifEmpty { appLabel }

    val isShortcut: Boolean get() = shortcutId != null

    /** Key for the drawer name, keywords and the hidden flag; the plain package name for real apps. */
    val prefKey: String get() = appPrefKey(appPackage, shortcutId)

    /** Unique across profiles, also for several shortcuts of one package. */
    val identity: String get() = "$prefKey|$user"
}

fun appPrefKey(appPackage: String, shortcutId: String?): String =
    if (shortcutId == null) appPackage else "$appPackage/shortcut/$shortcutId"
