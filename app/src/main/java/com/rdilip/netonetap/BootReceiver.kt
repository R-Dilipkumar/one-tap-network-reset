package com.rdilip.netonetap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Brings the 5G notification back after a reboot or app update. */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
      intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
    ) return
    if (Sims.hasPhonePermission(context)) NrMonitorService.start(context)
  }
}
