package com.tubers.blacklight.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/**
 * A seconds counter that advances once per frame while [running]. The draw code reads it inside
 * Canvas, so only drawing is invalidated (no recomposition per frame). It stops completely when
 * not running, which is what makes reduced motion and backgrounding cost nothing.
 */
@Composable
fun rememberAnimationClock(running: Boolean): MutableFloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0L) {
                    // Clamp large gaps (e.g. returning from background) so effects don't jump.
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                    seconds.floatValue += dt
                }
                last = now
            }
        }
    }
    return seconds
}

/**
 * Whether light animations should run: the light is on, the user hasn't asked for reduced
 * motion (in-app or system-wide "Remove animations"), and the screen is visible.
 */
@Composable
fun shouldAnimate(isOn: Boolean, reducedMotion: Boolean): Boolean {
    val context = LocalContext.current
    var animatorScale by remember { mutableStateOf(readAnimatorScale(context)) }
    // Re-read on every resume, so turning "Remove animations" on or off in system settings
    // applies as soon as the user comes back, without restarting the app.
    LifecycleResumeEffect(context) {
        animatorScale = readAnimatorScale(context)
        onPauseOrDispose { }
    }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visible = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    return animationAllowed(isOn, reducedMotion, animatorScale, visible)
}

private fun readAnimatorScale(context: Context): Float =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

/** The rule behind [shouldAnimate]; an animator scale of 0 is the system "Remove animations" switch. */
fun animationAllowed(isOn: Boolean, reducedMotion: Boolean, animatorScale: Float, visible: Boolean): Boolean =
    isOn && !reducedMotion && animatorScale != 0f && visible
