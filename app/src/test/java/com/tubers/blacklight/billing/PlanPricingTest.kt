package com.tubers.blacklight.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanPricingTest {

    private fun phase(price: String, micros: Long, period: String, cycles: Int = 0, forever: Boolean = false) =
        PricePhase(price, micros, "USD", period, cycles, forever)

    private val yearly = phase("$19.99", 19_990_000, "P1Y", forever = true)
    private val monthly = phase("$2.99", 2_990_000, "P1M", forever = true)

    private fun offer(token: String, vararg phases: PricePhase) = PlanOffer("uvglow_premium", token, phases.toList())

    @Test fun picksTrialThenIntroThenBasePlan() {
        val base = offer("base", yearly)
        val intro = offer("intro", phase("$9.99", 9_990_000, "P1Y", cycles = 1), yearly)
        val trial = offer("trial", phase("Free", 0, "P7D", cycles = 1), yearly)
        assertEquals("trial", PlanPricing.pickOffer(listOf(base, intro, trial))?.offerToken)
        assertEquals("intro", PlanPricing.pickOffer(listOf(base, intro))?.offerToken)
        assertEquals("base", PlanPricing.pickOffer(listOf(base))?.offerToken)
    }

    @Test fun ignoresOffersThatDoNotRenew() {
        val prepaid = offer("prepaid", phase("$19.99", 19_990_000, "P1Y", cycles = 1))
        assertNull(PlanPricing.pickOffer(listOf(prepaid)))
        assertNull(PlanPricing.pickOffer(emptyList()))
    }

    @Test fun periodsReadAsWords() {
        assertEquals("month", PlanPricing.periodUnit("P1M"))
        assertEquals("year", PlanPricing.periodUnit("P1Y"))
        assertEquals("3 months", PlanPricing.periodUnit("P3M"))
        assertEquals("week", PlanPricing.periodUnit("P1W"))
        assertEquals("7 days", PlanPricing.duration("P7D"))
        assertEquals("1 month", PlanPricing.duration("P1M"))
        assertEquals("3 months", PlanPricing.duration("P1M", cycles = 3))
        assertEquals("P1Y2M", PlanPricing.periodUnit("P1Y2M")) // unusual periods pass through unchanged
    }

    @Test fun recurringLabelUsesPlaysFormattedPrice() {
        assertEquals("$19.99 / year", PlanPricing.recurringLabel(offer("t", yearly)))
        assertEquals("$2.99 / month", PlanPricing.recurringLabel(offer("t", monthly)))
    }

    @Test fun introLabelsDescribeTrialAndPriceAfter() {
        assertNull(PlanPricing.introLabel(offer("t", yearly)))
        assertEquals(
            "Free for 7 days, then $19.99 / year",
            PlanPricing.introLabel(offer("t", phase("Free", 0, "P7D", cycles = 1), yearly)),
        )
        assertEquals(
            "$0.99 / month for 3 months, then $2.99 / month",
            PlanPricing.introLabel(offer("t", phase("$0.99", 990_000, "P1M", cycles = 3), monthly)),
        )
        val both = PlanPricing.introLabel(
            offer("t", phase("Free", 0, "P1W", cycles = 1), phase("$9.99", 9_990_000, "P1Y", cycles = 1), yearly),
        )!!
        assertTrue(both.startsWith("Free for 1 week, $9.99 for the first 1 year"))
        assertTrue(both.endsWith("then $19.99 / year"))
    }
}
