package com.citypeople.project.utilities

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View

class SafeOnTouchListener(
        private var defaultInterval: Int = 1000,
        private val onSafeCLick: (View) -> Unit
) : View.OnTouchListener {
    private var lastTimeClicked: Long = 0
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return false
        }
        lastTimeClicked = SystemClock.elapsedRealtime()
        v?.let { onSafeCLick(it) }
        return true
    }
}