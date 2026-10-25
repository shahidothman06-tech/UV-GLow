package com.tubers.blacklight.hardware

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Uses [CameraManager.setTorchMode], which does not require the CAMERA permission.
 */
class AndroidTorchController(context: Context) : TorchController {

    private val cameraManager: CameraManager? = context.getSystemService(CameraManager::class.java)
    private val torchState = MutableStateFlow(false)
    override val isTorchOn: StateFlow<Boolean> = torchState.asStateFlow()

    private val cameraId: String? = findFlashCamera(context)
    override val isAvailable: Boolean get() = cameraId != null

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) torchState.value = enabled
        }

        override fun onTorchModeUnavailable(id: String) {
            // The camera is in use by another app; the LED is off for us.
            if (id == cameraId) torchState.value = false
        }
    }

    init {
        if (cameraId != null) {
            runCatching { cameraManager?.registerTorchCallback(callback, Handler(Looper.getMainLooper())) }
        }
    }

    override fun setTorch(on: Boolean): Result<Unit> {
        val manager = cameraManager ?: return Result.failure(IllegalStateException("Camera service unavailable"))
        val id = cameraId ?: return Result.failure(IllegalStateException("This phone has no flashlight LED"))
        return try {
            manager.setTorchMode(id, on)
            Result.success(Unit)
        } catch (e: CameraAccessException) {
            Result.failure(IllegalStateException("The camera is busy in another app", e))
        } catch (e: IllegalArgumentException) {
            Result.failure(IllegalStateException("Flashlight not supported", e))
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException("Flashlight access was blocked", e))
        }
    }

    private fun findFlashCamera(context: Context): String? {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) return null
        val manager = cameraManager ?: return null
        return try {
            val withFlash = manager.cameraIdList.filter { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            // Prefer the rear camera's LED.
            withFlash.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            } ?: withFlash.firstOrNull()
        } catch (e: CameraAccessException) {
            null
        } catch (e: RuntimeException) {
            null
        }
    }
}
