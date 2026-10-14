package com.rdilip.netonetap

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.rdilip.netonetap.priv.PrivOps.TAG

/**
 * Keeps a notification that lists every SIM whose NR State (NSA) is CONNECTED and whose
 * NR Frequency is HIGH. SIMs that do not match are left out.
 */
class NrMonitorService : Service() {
  private val tms = mutableListOf<Pair<TelephonyManager, Any>>() // manager to its listener
  private val unlimitedBySlot = sortedMapOf<Int, Boolean>()
  private var subsListener: SubscriptionManager.OnSubscriptionsChangedListener? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    running = true
    goForeground()
    watchSimChanges()
    registerSims()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    goForeground()
    if (intent?.action == ACTION_RESET) ResetEngine.runAsync(this)
    return START_STICKY
  }

  override fun onDestroy() {
    running = false
    unregisterSims()
    subsListener?.let { getSystemService(SubscriptionManager::class.java)?.removeOnSubscriptionsChangedListener(it) }
    super.onDestroy()
  }

  private fun goForeground() {
    val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
    ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
  }

  private fun watchSimChanges() {
    val sm = getSystemService(SubscriptionManager::class.java) ?: return
    val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
      override fun onSubscriptionsChanged() = registerSims()
    }
    try {
      if (Build.VERSION.SDK_INT >= 30) {
        sm.addOnSubscriptionsChangedListener(mainExecutor, listener)
      } else {
        @Suppress("DEPRECATION")
        sm.addOnSubscriptionsChangedListener(listener)
      }
      subsListener = listener
    } catch (e: SecurityException) {
      Log.w(TAG, "Cannot watch SIM changes: ${e.message}")
    }
  }

  private fun registerSims() {
    unregisterSims()
    val base = getSystemService(TelephonyManager::class.java) ?: return
    for (info in Sims.active(this)) {
      val slot = info.simSlotIndex
      val tm = base.createForSubscriptionId(info.subscriptionId)
      val onState = { ss: ServiceState -> onServiceState(info.subscriptionId, slot, ss) }
      val listener: Any = if (Build.VERSION.SDK_INT >= 31) {
        val cb = object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
          override fun onServiceStateChanged(serviceState: ServiceState) = onState(serviceState)
        }
        tm.registerTelephonyCallback(mainExecutor, cb)
        cb
      } else {
        @Suppress("DEPRECATION")
        val l = object : PhoneStateListener(mainExecutor) {
          @Deprecated("Deprecated in Java")
          override fun onServiceStateChanged(serviceState: ServiceState?) {
            serviceState?.let(onState)
          }
        }
        @Suppress("DEPRECATION")
        tm.listen(l, PhoneStateListener.LISTEN_SERVICE_STATE)
        l
      }
      tms += tm to listener
    }
    refresh()
  }

  private fun unregisterSims() {
    for ((tm, listener) in tms) {
      if (Build.VERSION.SDK_INT >= 31 && listener is TelephonyCallback) {
        tm.unregisterTelephonyCallback(listener)
      } else if (listener is PhoneStateListener) {
        @Suppress("DEPRECATION")
        tm.listen(listener, PhoneStateListener.LISTEN_NONE)
      }
    }
    tms.clear()
    unlimitedBySlot.clear()
  }

  private fun onServiceState(subId: Int, slot: Int, ss: ServiceState) {
    val unlimited = NrReader.isUnlimited5g(ss)
    Log.d(
      TAG,
      "sub=$subId slot=$slot nrState=${NrReader.nrState(ss)} " +
        "nrFreq=${NrReader.nrFrequencyRange(ss)} -> unlimited=$unlimited",
    )
    if (unlimitedBySlot[slot] == unlimited) return
    unlimitedBySlot[slot] = unlimited
    refresh()
  }

  private fun refresh() = goForeground()

  private fun buildNotification(): Notification {
    val lines = unlimitedBySlot.filterValues { it }.keys.map { getString(R.string.notif_sim_line, it + 1) }
    val title: String
    val text: String
    when {
      !Sims.hasPhonePermission(this) -> {
        title = getString(R.string.notif_inactive)
        text = getString(R.string.notif_need_phone_perm)
      }
      !NrReader.isSupported -> {
        title = getString(R.string.notif_inactive)
        text = getString(R.string.notif_unsupported)
      }
      lines.isEmpty() -> {
        title = getString(R.string.notif_inactive)
        text = ""
      }
      lines.size == 1 -> {
        title = lines[0]
        text = ""
      }
      else -> {
        title = getString(R.string.notif_active)
        text = lines.joinToString("\n")
      }
    }

    val reset = PendingIntent.getService(
      this, 0,
      Intent(this, NrMonitorService::class.java).setAction(ACTION_RESET),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    return NotificationCompat.Builder(this, App.CHANNEL_STATUS)
      .setSmallIcon(R.drawable.ic_signal)
      .setContentTitle(title)
      .setContentText(text.substringBefore('\n'))
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setShowWhen(false)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .addAction(R.drawable.ic_reset, getString(R.string.notif_action_reset), reset)
      .build()
  }

  companion object {
    const val ACTION_RESET = "com.rdilip.netonetap.RESET"
    private const val NOTIFICATION_ID = 5

    @Volatile
    var running = false
      private set

    fun start(context: Context) {
      ContextCompat.startForegroundService(context, Intent(context, NrMonitorService::class.java))
    }
  }
}
