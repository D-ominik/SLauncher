package de.juvento.plain.listener

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.abs

/**
 * Swipe, double tap, click and long press detection for a view.
 * All callbacks run on the main thread.
 */
internal open class SwipeTouchListener(context: Context?, private val view: View? = null) : OnTouchListener {

    private val gestureDetector = GestureDetector(context, GestureListener())

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> view?.isPressed = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view?.isPressed = false
        }
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onLongClick()
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null) return false
            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x
            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                    if (diffX > 0) onSwipeRight() else onSwipeLeft()
                }
            } else if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                if (diffY < 0) onSwipeUp() else onSwipeDown()
            }
            return false
        }
    }

    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}
    open fun onLongClick() {}
    open fun onDoubleClick() {}
    open fun onClick() {}
}
