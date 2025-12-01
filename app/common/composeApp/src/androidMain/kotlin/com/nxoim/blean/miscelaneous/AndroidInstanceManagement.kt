package com.nxoim.blean.miscelaneous

import android.app.Activity
import com.nxoim.blean.shared.PlatformInstanceManagement

class AndroidInstanceManagement(val activity: Activity) : PlatformInstanceManagement {
    override fun close() {
        activity.finishAndRemoveTask()
    }
}