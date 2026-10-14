package com.rdilip.netonetap

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.rdilip.netonetap.priv.PrivOps
import com.rdilip.netonetap.priv.PrivOps.TAG
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * One-tap reset. Tries, in order:
 *  1. Shizuku (shell rights): cellular radio off/on through ITelephony.
 *  2. Root: the same code, run as root through app_process.
 *  3. Neither: ask Android to re-check the active network (weak, but needs no rights).
 * Wi-Fi, Bluetooth, airplane mode and the mobile data switch are never touched.
 */
object ResetEngine {
  private val busy = AtomicBoolean(false)
  private val main = Handler(Looper.getMainLooper())

  fun runAsync(context: Context) {
    val app = context.applicationContext
    if (!busy.compareAndSet(false, true)) {
      toast(app, app.getString(R.string.reset_busy))
      return
    }
    thread(name = "net-reset") {
      try {
        run(app)
      } catch (t: Throwable) {
        Log.e(TAG, "Reset failed", t)
        toast(app, app.getString(R.string.reset_failed, t.message ?: t.javaClass.simpleName))
      } finally {
        busy.set(false)
      }
    }
  }

  private fun run(app: Context) {
    if (inCall(app)) {
      toast(app, app.getString(R.string.reset_in_call))
      return
    }
    val subs = Sims.active(app).map { it.subscriptionId }.toIntArray()

    val result = when {
      ShizukuHelper.isInstalled(app) && ShizukuHelper.awaitBinder(1500) &&
        ShizukuHelper.hasPermission() -> {
        toast(app, app.getString(R.string.reset_started))
        "shizuku: " + PrivOps.cycle(PrivOps.telephony(ShizukuHelper.phoneBinder()), subs)
      }
      RootHelper.isAvailable() -> {
        toast(app, app.getString(R.string.reset_started))
        "root: " + RootHelper.cycle(app, subs)
      }
      else -> {
        softRefresh(app)
        toast(app, app.getString(R.string.reset_no_privilege))
        return
      }
    }
    Log.i(TAG, result)

    val back = waitForMobileData(app, timeoutMs = 20_000)
    toast(app, app.getString(if (back) R.string.reset_done else R.string.reset_radio_back))
  }

  private fun inCall(app: Context): Boolean = try {
    app.getSystemService(TelecomManager::class.java)?.isInCall == true
  } catch (_: SecurityException) {
    false
  }

  /** True once the data SIM has a data connection again. */
  private fun waitForMobileData(app: Context, timeoutMs: Long): Boolean {
    val dataSub = SubscriptionManager.getDefaultDataSubscriptionId()
    val tm = app.getSystemService(TelephonyManager::class.java)?.let {
      if (dataSub != SubscriptionManager.INVALID_SUBSCRIPTION_ID) it.createForSubscriptionId(dataSub) else it
    } ?: return false
    val dataOn = try {
      tm.isDataEnabled
    } catch (_: SecurityException) {
      true
    }
    if (!dataOn) return false

    Thread.sleep(1500)
    val end = SystemClock.uptimeMillis() + timeoutMs
    while (SystemClock.uptimeMillis() < end) {
      if (tm.dataState == TelephonyManager.DATA_CONNECTED) return true
      Thread.sleep(500)
    }
    return false
  }

  private fun softRefresh(app: Context) {
    val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
    cm.activeNetwork?.let { cm.reportNetworkConnectivity(it, false) }
    Log.i(TAG, "soft refresh: reported active network as bad")
  }

  private fun toast(app: Context, text: String) {
    main.post { Toast.makeText(app, text, Toast.LENGTH_SHORT).show() }
  }
}
