package com.tubers.blacklight

/** External pages the app opens. Kept in one place so release checks only need to look here. */
object AppLinks {

    /**
     * Public privacy policy page entered in Play Console › App content › Privacy policy. The app
     * itself shows the policy in-app (PrivacyPolicyScreen) and doesn't open this. Published from
     * docs/privacy-policy.md by GitHub Pages (main branch, /docs folder).
     */
    const val PRIVACY_POLICY_URL = "https://shahidothman06-tech.github.io/UV-GLow/privacy-policy"

    /**
     * Shown in the privacy policy's Contact section (in-app and in docs/privacy-policy.md).
     */
    const val CONTACT_EMAIL = "mailtosarwar359@gmail.com"

    const val PACKAGE_NAME = "com.tubers.blacklight"

    /**
     * The app's Play Store page, used by Settings › Rate us. The market:// link opens the Play
     * Store app; the https link is the fallback when Play isn't installed.
     */
    val playStoreUrls: List<String> = listOf(
        "market://details?id=$PACKAGE_NAME",
        "https://play.google.com/store/apps/details?id=$PACKAGE_NAME",
    )

    private const val SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"

    /**
     * Play's subscription management page. With a product ID it opens that subscription directly;
     * without one (nothing active, or not known yet) it opens the list of all subscriptions.
     * Play product IDs only use a-z, 0-9, "_" and ".", so no URL encoding is needed.
     */
    fun manageSubscriptionUrl(productId: String?): String =
        if (productId.isNullOrBlank()) {
            SUBSCRIPTIONS_URL
        } else {
            "$SUBSCRIPTIONS_URL?sku=$productId&package=$PACKAGE_NAME"
        }
}
