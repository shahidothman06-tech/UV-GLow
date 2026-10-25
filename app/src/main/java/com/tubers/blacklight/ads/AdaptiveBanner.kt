package com.tubers.blacklight.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * One anchored adaptive banner, as wide as the space it's given. Its height is reserved up front,
 * so content above never jumps or gets covered when the ad loads.
 */
@Composable
fun AdaptiveBanner(adUnitId: String, background: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().background(background)) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
        key(adUnitId, adSize) {
            val adView = remember {
                AdView(context).apply {
                    setAdSize(adSize)
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                }
            }
            // Pause refreshes while the app isn't visible; destroy when the banner leaves the screen.
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle, adView) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> adView.pause()
                        Lifecycle.Event.ON_RESUME -> adView.resume()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
                    adView.destroy()
                }
            }
            AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(adSize.height.dp))
        }
    }
}
