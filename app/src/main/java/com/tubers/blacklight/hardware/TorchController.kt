package com.tubers.blacklight.hardware

import kotlinx.coroutines.flow.StateFlow

/**
 * Optional control of the phone's camera LED. The LED gives ordinary white light — it is
 * never UV. Kept behind an interface so the ViewModel can be tested without hardware.
 */
interface TorchController {
    /** False when the device has no usable flash. */
    val isAvailable: Boolean
    /** Actual LED state as reported by the system (another app can change it). */
    val isTorchOn: StateFlow<Boolean>
    fun setTorch(on: Boolean): Result<Unit>
}
