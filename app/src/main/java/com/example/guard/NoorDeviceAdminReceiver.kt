package com.example.guard

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class NoorDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val prefs = GuardPreferences(context)
        if (prefs.getCurrentState().uninstallLockEnabled) {
            prefs.setDirectPushUpLock(com.example.data.model.LockMode.PUSHUP_30, 30)
            val lockIntent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(lockIntent)
        }
        return "30 Ta Push-Up Maren - Phone Khulbe"
    }
}
