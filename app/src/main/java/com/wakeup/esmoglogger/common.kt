package com.wakeup.esmoglogger

import android.graphics.Color
import android.graphics.PorterDuff
import com.google.android.material.button.MaterialButton

fun ButtonSetEnabled(button: MaterialButton?, enabled: Boolean) {
    button?.isEnabled = enabled
    if (enabled) {
        button?.icon?.clearColorFilter()
    } else {
        button?.icon?.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
    }
}