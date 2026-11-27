package de.juvento.plain.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ScrollView
import kotlin.math.abs

/**
 * ScrollView that only takes part in touch handling when its content really overflows.
 * Otherwise all touches fall through to the views behind it, so the home screen gestures
 * (swipes, double tap, long press) keep working on empty areas.
 * While it scrolls, touches on empty areas are still passed to [emptyAreaTouchListener] until the
 * finger moves vertically, so horizontal swipes, double tap and long press keep working.
 */
class HomeScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ScrollView(context, attrs) {

    var emptyAreaTouchListener: View.OnTouchListener? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var forwarding = false
    private var downY = 0f

    private fun overflows(): Boolean {
        val content = getChildAt(0) ?: return false
        return content.height > height - paddingTop - paddingBottom
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = overflows() && super.onInterceptTouchEvent(ev)

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!overflows()) return false
        forwardToEmptyAreaListener(ev)
        return super.onTouchEvent(ev)
    }

    private fun forwardToEmptyAreaListener(ev: MotionEvent) {
        val listener = emptyAreaTouchListener ?: return
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                forwarding = true
                downY = ev.y
            }
            MotionEvent.ACTION_MOVE -> if (forwarding && abs(ev.y - downY) > touchSlop) {
                cancelForwarding(listener, ev)
                return
            }
        }
        if (forwarding) listener.onTouch(this, ev)
        if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) forwarding = false
    }

    // The scroll gesture wins over the empty area gestures once the finger moves vertically.
    private fun cancelForwarding(listener: View.OnTouchListener, ev: MotionEvent) {
        val cancel = MotionEvent.obtain(ev)
        cancel.action = MotionEvent.ACTION_CANCEL
        listener.onTouch(this, cancel)
        cancel.recycle()
        forwarding = false
    }
}
