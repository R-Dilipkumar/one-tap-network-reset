package com.rdilip.netonetap

import android.content.Context
import android.util.Log
import com.rdilip.netonetap.priv.PrivOps.TAG
import com.rdilip.netonetap.priv.RootMain
import java.util.concurrent.TimeUnit

object RootHelper {
  /** Runs `su -c id`. The first call may show the root manager's allow prompt. */
  fun isAvailable(): Boolean {
    val (code, out) = su("id", timeoutSec = 20) ?: return false
    return code == 0 && out.contains("uid=0")
  }

  /** Runs [RootMain] as root through app_process, using this app's own APK. */
  fun cycle(context: Context, subIds: IntArray): String {
    val apk = context.applicationInfo.sourceDir
    val cmd = "CLASSPATH='$apk' /system/bin/app_process /system/bin " +
      "${RootMain::class.java.name} cycle ${subIds.joinToString(",")}"
    val (code, out) = su(cmd, timeoutSec = 40) ?: error("su did not run")
    if (code != 0) error(out.ifBlank { "root command failed ($code)" })
    return out
  }

  private fun su(cmd: String, timeoutSec: Long): Pair<Int, String>? = try {
    val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
    val out = StringBuilder()
    val reader = Thread { p.inputStream.bufferedReader().forEachLine { out.appendLine(it) } }
    reader.start()
    if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
      p.destroy()
      null
    } else {
      reader.join(1000)
      p.exitValue() to out.toString().trim()
    }
  } catch (e: Exception) {
    Log.d(TAG, "su not usable: ${e.message}")
    null
  }
}
