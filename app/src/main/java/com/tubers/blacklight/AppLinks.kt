package com.tubers.blacklight

/** External pages the app opens. Kept in one place so release checks only need to look here. */
object AppLinks {

    /**
     * Public privacy policy page entered in Play Console › App content › Privacy policy. The app
     * itself shows the policy in-app (PrivacyPolicyScreen) and doesn't open this.
     * TODO before release: host docs/privacy-policy.md and put its https URL here.
     */
    const val PRIVACY_POLICY_URL = ""

    /**
     * Shown in the privacy policy's Contact section (in-app and in docs/privacy-policy.md).
     * TODO before release: replace with your real support address.
     */
    const val CONTACT_EMAIL = "your-email@example.com"

    const val PACKAGE_NAME = "com.tubers.blacklight"

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
