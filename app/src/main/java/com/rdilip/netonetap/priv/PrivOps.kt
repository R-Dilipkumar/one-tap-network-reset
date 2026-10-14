package com.rdilip.netonetap.priv

import android.os.IBinder
import android.util.Log
import java.lang.reflect.InvocationTargetException

/**
 * Calls the phone service (ITelephony) directly. Needs MODIFY_PHONE_STATE, which the
 * shell user (Shizuku) and root have, but normal apps do not.
 *
 * Uses only framework classes and no Context, so it can also run inside a root
 * app_process (see [RootMain]).
 */
object PrivOps {
  const val TAG = "NetOneTap"

  private const val RADIO_OFF_MILLIS = 2500L
  private const val NUDGE_MILLIS = 3000L

  // TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER
  private const val REASON_USER = 0

  // TelephonyManager.NETWORK_TYPE_BITMASK_NR = 1 << (NETWORK_TYPE_NR - 1), NETWORK_TYPE_NR = 20
  private const val NR_BITMASK = 1L shl 19

  /** Wraps the "phone" service binder in the device's own ITelephony proxy. */
  fun telephony(phoneBinder: IBinder): Any {
    val stub = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
    return stub.getMethod("asInterface", IBinder::class.java).invoke(null, phoneBinder)
      ?: error("ITelephony is null")
  }

  /**
   * Turns the cellular radio off and on again for each SIM (airplane mode for cellular only).
   * If that is not allowed, briefly removes 5G from the allowed network types instead.
   * Returns a one-line summary.
   */
  fun cycle(telephony: Any, subIds: IntArray): String {
    val turnedOff = mutableListOf<Int>()
    try {
      if (subIds.isEmpty()) {
        if (setRadio(telephony, null, false)) turnedOff += -1
      } else {
        for (sub in subIds) if (setRadio(telephony, sub, false)) turnedOff += sub
      }
      if (turnedOff.isNotEmpty()) Thread.sleep(RADIO_OFF_MILLIS)
    } finally {
      // Always switch the radio back on, even if something above failed.
      for (sub in turnedOff) setRadio(telephony, sub.takeIf { it >= 0 }, true)
    }
    if (turnedOff.isNotEmpty()) return "radio-cycle ok (${turnedOff.size} SIM)"

    Log.w(TAG, "Radio cycle not possible, trying 5G nudge")
    val nudged = subIds.count { nudge(telephony, it) }
    if (nudged > 0) return "5g-nudge ok ($nudged SIM)"
    error("No reset method worked")
  }

  /** subId == null means the default phone. */
  private fun setRadio(telephony: Any, subId: Int?, on: Boolean): Boolean {
    val c = telephony.javaClass
    return try {
      val result = if (subId != null) {
        try {
          c.getMethod("setRadioForSubscriber", Int::class.java, Boolean::class.java)
            .invoke(telephony, subId, on)
        } catch (_: NoSuchMethodException) {
          c.getMethod("setRadio", Boolean::class.java).invoke(telephony, on)
        }
      } else {
        c.getMethod("setRadio", Boolean::class.java).invoke(telephony, on)
      }
      Log.i(TAG, "setRadio sub=$subId on=$on -> $result")
      result == true
    } catch (t: Throwable) {
      Log.w(TAG, "setRadio sub=$subId on=$on failed: ${rootMessage(t)}")
      false
    }
  }

  /** Android 12+: drop NR from the allowed network types for a moment, then restore it. */
  private fun nudge(telephony: Any, subId: Int): Boolean {
    val c = telephony.javaClass
    return try {
      val get = c.getMethod("getAllowedNetworkTypesForReason", Int::class.java, Int::class.java)
      val set = c.getMethod(
        "setAllowedNetworkTypesForReason", Int::class.java, Int::class.java, Long::class.java,
      )
      val original = get.invoke(telephony, subId, REASON_USER) as Long
      if (original and NR_BITMASK == 0L) return false
      set.invoke(telephony, subId, REASON_USER, original and NR_BITMASK.inv())
      try {
        Thread.sleep(NUDGE_MILLIS)
      } finally {
        set.invoke(telephony, subId, REASON_USER, original)
      }
      Log.i(TAG, "nudge sub=$subId done")
      true
    } catch (t: Throwable) {
      Log.w(TAG, "nudge sub=$subId failed: ${rootMessage(t)}")
      false
    }
  }

  private fun rootMessage(t: Throwable): String {
    val cause = if (t is InvocationTargetException) t.targetException ?: t else t
    return "${cause.javaClass.simpleName}: ${cause.message}"
  }
}
