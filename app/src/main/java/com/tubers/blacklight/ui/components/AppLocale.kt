package com.tubers.blacklight.ui.components

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/**
 * Sets the app's own language (Android 13+ per-app language, also shown in system Settings ›
 * Apps › UV Glow › Language). Older versions keep the device language. Android recreates the
 * activity when the language actually changes.
 */
fun Context.applyAppLanguage(tag: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val manager = getSystemService(LocaleManager::class.java) ?: return
    val wanted = LocaleList.forLanguageTags(tag)
    if (manager.applicationLocales != wanted) manager.applicationLocales = wanted
}
