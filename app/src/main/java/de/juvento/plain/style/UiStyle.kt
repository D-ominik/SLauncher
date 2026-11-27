package de.juvento.plain.style

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import de.juvento.plain.data.Prefs

/** Applies the user-selected font and text color to the classic (View based) launcher screens. */
object UiStyle {

    private const val PRESSED_ALPHA = 0xCC

    fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** The configured color, or black (light theme) / white (dark theme) when set to automatic. */
    fun effectiveColor(night: Boolean, custom: Int?): Int =
        custom ?: if (night) Color.WHITE else Color.BLACK

    fun textColor(context: Context, prefs: Prefs): Int {
        val night = isNight(context)
        return effectiveColor(night, if (night) prefs.textColorDark else prefs.textColorLight)
    }

    fun colorStateList(color: Int): ColorStateList = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf()),
        intArrayOf(ColorUtils.setAlphaComponent(color, PRESSED_ALPHA), color)
    )

    /** Snapshot of font and color so that many views can be styled without re-reading prefs. */
    class Snapshot(val typeface: Typeface, val color: Int) {
        fun apply(vararg views: TextView?) {
            for (view in views) {
                view ?: continue
                view.typeface = typeface
                view.setTextColor(colorStateList(color))
            }
        }
    }

    fun snapshot(context: Context, prefs: Prefs = Prefs(context)) =
        Snapshot(prefs.font.typeface(), textColor(context, prefs))
}
