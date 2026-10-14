package com.rdilip.netonetap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager

object Sims {
  fun hasPhonePermission(context: Context) =
    context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
      PackageManager.PERMISSION_GRANTED

  /** Active SIMs, sorted by slot. Empty without phone permission. */
  fun active(context: Context): List<SubscriptionInfo> {
    if (!hasPhonePermission(context)) return emptyList()
    val sm = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
    return try {
      sm.activeSubscriptionInfoList.orEmpty().sortedBy { it.simSlotIndex }
    } catch (_: SecurityException) {
      emptyList()
    }
  }
}
