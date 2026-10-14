package com.rdilip.netonetap

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    // Lets NrReader and PrivOps call hidden telephony methods by reflection.
    HiddenApiBypass.addHiddenApiExemptions("Landroid/telephony/", "Lcom/android/internal/telephony/")

    val channel = NotificationChannel(
      CHANNEL_STATUS, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = getString(R.string.channel_desc)
      setShowBadge(false)
    }
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  companion object {
    const val CHANNEL_STATUS = "status"
  }
}
