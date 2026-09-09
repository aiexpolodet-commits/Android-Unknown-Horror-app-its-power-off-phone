package com.example

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

object SystemPowerOffHelper {

    /**
     * Executes phone power off / screen lock / shutdown commands across multiple layers:
     * 1. Shell reboot -p / shutdown (if root / emulator shell allowed)
     * 2. DevicePolicyManager lockNow() (powers off the screen / locks device instantly)
     * 3. Activity window brightness to 0 and exitProcess
     */
    fun performPowerOff(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(context, HorrorDeviceAdminReceiver::class.java)

        // 1. Try real root/shell shutdown/reboot -p command
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot -p"))
            } catch (_: Exception) {}
            try {
                Runtime.getRuntime().exec(arrayOf("reboot", "-p"))
            } catch (_: Exception) {}
            try {
                Runtime.getRuntime().exec(arrayOf("shutdown", "-P", "now"))
            } catch (_: Exception) {}
            try {
                Runtime.getRuntime().exec(arrayOf("setprop", "sys.powerctl", "shutdown"))
            } catch (_: Exception) {}
        }

        // 2. Lock / turn off the device screen immediately if admin is enabled
        try {
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
            }
        } catch (_: Exception) {}

        // 3. Set display brightness to absolute 0
        (context as? Activity)?.runOnUiThread {
            try {
                val window = (context as Activity).window
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
                window.attributes = lp
            } catch (_: Exception) {}
        }

        // 4. Terminate process after triggering
        CoroutineScope(Dispatchers.Main).launch {
            delay(1200L)
            (context as? Activity)?.finishAffinity()
            delay(300L)
            exitProcess(0)
        }
    }

    /**
     * Optional helper to request Device Admin permission if not granted yet
     */
    fun ensureDeviceAdminPrompt(activity: Activity) {
        try {
            val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComponent = ComponentName(activity, HorrorDeviceAdminReceiver::class.java)
            if (dpm != null && !dpm.isAdminActive(adminComponent)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Allow Unknown system service to control power status"
                    )
                }
                activity.startActivity(intent)
            }
        } catch (_: Exception) {}
    }
}
