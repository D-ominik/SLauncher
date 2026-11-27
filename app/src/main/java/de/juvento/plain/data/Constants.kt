package de.juvento.plain.data

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.juvento.plain.R

interface EnumOption {
    @Composable
    fun string(): String
}

object Constants {

    const val ACTION_SET_ACTIVE_SET = "de.juvento.plain.action.SET_ACTIVE_SET"
    const val EXTRA_SET_NAME = "set"

    const val TEXT_SIZE_MIN = 10
    const val TEXT_SIZE_MAX = 60
    const val TEXT_SIZE_DEFAULT = 18

    enum class AppDrawerFlag {
        LaunchApp,
        HiddenApps,
    }

    /** [tag] is a BCP 47 language tag; an empty tag means "follow the system". */
    data class Language(val tag: String, val nativeName: String?) {
        companion object {
            val System = Language("", null)

            val all = listOf(
                System,
                Language("af", "Afrikaans"),
                Language("ar", "العربية"),
                Language("ca", "Català"),
                Language("cs", "Čeština"),
                Language("da", "Dansk"),
                Language("de", "Deutsch"),
                Language("el", "Ελληνικά"),
                Language("en", "English"),
                Language("es", "Español"),
                Language("et", "Eesti keel"),
                Language("fa", "فارسی"),
                Language("fi", "Suomi"),
                Language("fr", "Français"),
                Language("he", "עברית"),
                Language("hr", "Hrvatski"),
                Language("hu", "Magyar"),
                Language("id", "Bahasa Indonesia"),
                Language("it", "Italiano"),
                Language("ja", "日本語"),
                Language("ko", "한국어"),
                Language("lt", "Lietuvių"),
                Language("nl", "Nederlands"),
                Language("no", "Norsk"),
                Language("pl", "Polski"),
                Language("pt-BR", "Português (Brasil)"),
                Language("pt-PT", "Português (Portugal)"),
                Language("ro", "Română"),
                Language("ru", "Русский"),
                Language("sr", "Српски"),
                Language("sv", "Svenska"),
                Language("th", "ไทย"),
                Language("tr", "Türkçe"),
                Language("uk", "Українська"),
                Language("vi", "Tiếng Việt"),
                Language("zh-CN", "简体中文"),
                Language("zh-TW", "繁體中文"),
            )

            /** Maps an active tag to the closest entry of [all] (System if unknown). */
            fun fromTag(tag: String): Language {
                if (tag.isEmpty()) return System
                return all.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
                    ?: all.firstOrNull { it !== System && it.tag.substringBefore('-').equals(tag.substringBefore('-'), true) }
                    ?: System
            }
        }
    }

    enum class Gravity : EnumOption {
        Left,
        Center,
        Right;

        @Composable
        override fun string(): String {
            return when (this) {
                Left -> stringResource(R.string.left)
                Center -> stringResource(R.string.center)
                Right -> stringResource(R.string.right)
            }
        }

        @SuppressLint("RtlHardcoded")
        fun value(): Int {
            return when (this) {
                Left -> android.view.Gravity.LEFT
                Center -> android.view.Gravity.CENTER
                Right -> android.view.Gravity.RIGHT
            }
        }
    }

    enum class Action : EnumOption {
        Disabled,
        OpenApp,
        LockScreen,
        ShowAppList,
        OpenQuickSettings,
        ShowRecents,
        ShowNotification;

        @Composable
        override fun string(): String {
            return when (this) {
                OpenApp -> stringResource(R.string.open_app)
                LockScreen -> stringResource(R.string.lock_screen)
                ShowNotification -> stringResource(R.string.show_notifications)
                ShowAppList -> stringResource(R.string.show_app_list)
                OpenQuickSettings -> stringResource(R.string.open_quick_settings)
                ShowRecents -> stringResource(R.string.show_recents)
                Disabled -> stringResource(R.string.disabled)
            }
        }
    }

    enum class Theme : EnumOption {
        System,
        Dark,
        Light;

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.lang_system)
                Dark -> stringResource(R.string.dark)
                Light -> stringResource(R.string.light)
            }
        }
    }

    enum class Orientation(val requested: Int) : EnumOption {
        Auto(ActivityInfo.SCREEN_ORIENTATION_FULL_USER),
        Portrait(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT),
        Landscape(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);

        @Composable
        override fun string(): String = when (this) {
            Auto -> stringResource(R.string.orientation_auto)
            Portrait -> stringResource(R.string.orientation_portrait)
            Landscape -> stringResource(R.string.orientation_landscape)
        }
    }

    /** System font families, so no font files have to be bundled. */
    enum class AppFont(val family: String, val label: String) {
        SansLight("sans-serif-light", "Sans Light"),
        Sans("sans-serif", "Sans"),
        SansMedium("sans-serif-medium", "Sans Medium"),
        SansThin("sans-serif-thin", "Sans Thin"),
        SansCondensed("sans-serif-condensed", "Sans Condensed"),
        SansBlack("sans-serif-black", "Sans Black"),
        Serif("serif", "Serif"),
        Monospace("monospace", "Monospace"),
        Cursive("cursive", "Cursive"),
        Casual("casual", "Casual");

        fun typeface(): Typeface = Typeface.create(family, Typeface.NORMAL)
    }
}
