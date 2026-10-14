package com.rdilip.netonetap

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import kotlin.concurrent.thread

/**
 * Launcher tap. Shows nothing: asks for missing permissions (first run only),
 * makes sure the 5G notification is running, starts the reset, and closes.
 */
class ResetActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val missing = neededPermissions().filter {
      checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }
    if (missing.isEmpty()) afterPermissions() else requestPermissions(missing.toTypedArray(), REQ_PERMS)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode != REQ_PERMS) return
    if (Build.VERSION.SDK_INT >= 33 &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      Toast.makeText(this, R.string.need_notifications, Toast.LENGTH_LONG).show()
    }
    afterPermissions()
  }

  private fun afterPermissions() {
    NrMonitorService.start(this)
    thread {
      val shizukuUp = ShizukuHelper.isInstalled(this) && ShizukuHelper.awaitBinder(1500)
      runOnUiThread {
        if (shizukuUp && !ShizukuHelper.hasPermission()) {
          ShizukuHelper.requestPermission { resetAndFinish() }
        } else {
          resetAndFinish()
        }
      }
    }
  }

  private fun resetAndFinish() {
    ResetEngine.runAsync(this)
    finish()
  }

  private fun neededPermissions(): List<String> = buildList {
    add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
  }

  private companion object {
    const val REQ_PERMS = 1
  }
}
