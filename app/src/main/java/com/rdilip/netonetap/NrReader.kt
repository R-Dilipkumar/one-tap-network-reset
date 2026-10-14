package com.rdilip.netonetap

import android.telephony.ServiceState
import java.lang.reflect.Method

/**
 * Reads the same values the *#*#4636#*#* screen shows as "NR State (NSA)" and "NR Frequency".
 * Both getters are hidden, so they are called by reflection ([App] lifts the hidden-API block).
 */
object NrReader {
  // NetworkRegistrationInfo.NR_STATE_CONNECTED
  const val NR_STATE_CONNECTED = 3

  // ServiceState.FREQUENCY_RANGE_HIGH (3 GHz to 6 GHz)
  const val FREQUENCY_RANGE_HIGH = 3

  private val getNrState: Method? by lazy { hidden("getNrState") }
  private val getNrFrequencyRange: Method? by lazy { hidden("getNrFrequencyRange") }

  val isSupported: Boolean get() = getNrState != null && getNrFrequencyRange != null

  fun nrState(ss: ServiceState): Int? = call(getNrState, ss)

  fun nrFrequencyRange(ss: ServiceState): Int? = call(getNrFrequencyRange, ss)

  /** The user's rule: NR State (NSA) = CONNECTED and NR Frequency = HIGH. */
  fun isUnlimited5g(ss: ServiceState): Boolean =
    nrState(ss) == NR_STATE_CONNECTED && nrFrequencyRange(ss) == FREQUENCY_RANGE_HIGH

  private fun hidden(name: String): Method? = try {
    ServiceState::class.java.getDeclaredMethod(name).apply { isAccessible = true }
  } catch (_: Throwable) {
    null
  }

  private fun call(m: Method?, ss: ServiceState): Int? = try {
    m?.invoke(ss) as? Int
  } catch (_: Throwable) {
    null
  }
}
