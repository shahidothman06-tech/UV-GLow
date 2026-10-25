package com.tubers.blacklight.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri

/**
 * Opens [url] in the Play Store app or a browser. Returns false instead of crashing when nothing
 * can open it (no browser, a locked-down profile) or the URL hasn't been filled in yet.
 */
fun Context.openExternal(url: String): Boolean {
    if (url.isBlank()) return false
    return try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/** The Activity behind a Compose [Context], needed by Play Billing and Google's consent forms. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
