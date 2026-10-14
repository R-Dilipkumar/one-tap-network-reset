package com.rdilip.netonetap.priv

import android.os.IBinder
import kotlin.system.exitProcess

/**
 * Entry point for the root path. Started by [com.rdilip.netonetap.RootHelper] as:
 *   su -c "CLASSPATH=<apk> app_process /system/bin com.rdilip.netonetap.priv.RootMain cycle 1,2"
 */
object RootMain {
  @JvmStatic
  fun main(args: Array<String>) {
    try {
      require(args.firstOrNull() == "cycle") { "usage: cycle [subId,subId]" }
      val subs = args.getOrNull(1).orEmpty().split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .toIntArray()
      val binder = Class.forName("android.os.ServiceManager")
        .getMethod("getService", String::class.java)
        .invoke(null, "phone") as IBinder
      println(PrivOps.cycle(PrivOps.telephony(binder), subs))
      exitProcess(0)
    } catch (t: Throwable) {
      System.err.println("${t.javaClass.simpleName}: ${t.message}")
      exitProcess(1)
    }
  }
}
