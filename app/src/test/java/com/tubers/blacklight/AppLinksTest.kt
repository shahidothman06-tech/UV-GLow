package com.tubers.blacklight

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLinksTest {

    @Test fun manageSubscription_deepLinksToTheActiveProduct() {
        assertEquals(
            "https://play.google.com/store/account/subscriptions?sku=uvglow_premium_yearly&package=com.tubers.blacklight",
            AppLinks.manageSubscriptionUrl("uvglow_premium_yearly"),
        )
    }

    @Test fun manageSubscription_withoutProductOpensTheSubscriptionList() {
        assertEquals("https://play.google.com/store/account/subscriptions", AppLinks.manageSubscriptionUrl(null))
        assertEquals("https://play.google.com/store/account/subscriptions", AppLinks.manageSubscriptionUrl(""))
    }

    @Test fun packageNameMatchesTheBuild() {
        assertEquals(BuildConfig.APPLICATION_ID, AppLinks.PACKAGE_NAME)
    }
}
