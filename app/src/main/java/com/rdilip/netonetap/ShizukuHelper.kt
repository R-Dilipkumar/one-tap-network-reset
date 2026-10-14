package com.rdilip.netonetap

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.SystemClock
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object ShizukuHelper {
  private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
  private const val REQUEST_CODE = 4636

  fun isInstalled(context: Context): Boolean = try {
    context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
    true
  } catch (_: PackageManager.NameNotFoundException) {
    false
  }

  /**
   * Shizuku hands its binder to the app shortly after the process starts.
   * Polls for it; never call on the main thread.
   */
  fun awaitBinder(timeoutMs: Long): Boolean {
    val end = SystemClock.uptimeMillis() + timeoutMs
    while (true) {
      if (isRunning()) return true
      if (SystemClock.uptimeMillis() >= end) return false
      Thread.sleep(100)
    }
  }

  fun isRunning(): Boolean = try {
    Shizuku.pingBinder() && !Shizuku.isPreV11()
  } catch (_: Throwable) {
    false
  }

  fun hasPermission(): Boolean =
    isRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

  /** Shows Shizuku's own allow dialog. [onDone] runs on the main thread either way. */
  fun requestPermission(onDone: (granted: Boolean) -> Unit) {
    if (Shizuku.shouldShowRequestPermissionRationale()) {
      onDone(false) // user picked "deny and don't ask again"
      return
    }
    val listener = object : Shizuku.OnRequestPermissionResultListener {
      override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode != REQUEST_CODE) return
        Shizuku.removeRequestPermissionResultListener(this)
        onDone(grantResult == PackageManager.PERMISSION_GRANTED)
      }
    }
    Shizuku.addRequestPermissionResultListener(listener)
    Shizuku.requestPermission(REQUEST_CODE)
  }

  /** The "phone" system service, with every call made as the shell user. */
  fun phoneBinder(): IBinder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("phone"))
}
