package com.tubers.blacklight

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLinksTest {

    @Test fun manageSubscription_deepLinksToTheActiveProduct() {
        assertEquals(
            "https://play.google.com/store/account/subscriptions?sku=uvglow_premium_monthly&package=com.tubers.blacklight",
            AppLinks.manageSubscriptionUrl("uvglow_premium_monthly"),
        )
    }

    @Test fun manageSubscription_withoutProductOpensTheSubscriptionList() {
        assertEquals("https://play.google.com/store/account/subscriptions", AppLinks.manageSubscriptionUrl(null))
        assertEquals("https://play.google.com/store/account/subscriptions", AppLinks.manageSubscriptionUrl(""))
    }

    @Test fun rateUs_opensThePlayStoreAppFirstThenTheWebPage() {
        assertEquals(
            listOf(
                "market://details?id=com.tubers.blacklight",
                "https://play.google.com/store/apps/details?id=com.tubers.blacklight",
            ),
            AppLinks.playStoreUrls,
        )
    }

    @Test fun packageNameMatchesTheBuild() {
        assertEquals(BuildConfig.APPLICATION_ID, AppLinks.PACKAGE_NAME)
    }
}
