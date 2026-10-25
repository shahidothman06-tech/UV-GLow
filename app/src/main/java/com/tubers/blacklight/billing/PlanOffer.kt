package com.tubers.blacklight.billing

/**
 * One pricing phase of a subscription offer, copied out of Play's ProductDetails so the UI and
 * tests never touch Play classes. [billingPeriod] is ISO 8601 ("P1M", "P1Y", "P7D").
 */
data class PricePhase(
    /** Localised by Play, e.g. "€2,99" or "$19.99". Never build prices ourselves. */
    val formattedPrice: String,
    val priceMicros: Long,
    val currencyCode: String,
    val billingPeriod: String,
    /** How many periods this phase lasts; 0 for the open-ended recurring phase. */
    val cycles: Int,
    /** True for the phase that renews until cancelled (Play's INFINITE_RECURRING). */
    val renewsForever: Boolean,
) {
    val isFree: Boolean get() = priceMicros == 0L
}

/** The offer we sell for one plan: an optional trial / intro phase, then the renewing price. */
data class PlanOffer(
    val productId: String,
    val offerToken: String,
    val phases: List<PricePhase>,
) {
    /** The price that renews until cancelled. Always the last phase in Play's list. */
    val recurring: PricePhase get() = phases.last()

    /** Phases before the regular price (free trial and/or introductory price). */
    val introPhases: List<PricePhase> get() = phases.dropLast(1)

    val freeTrial: PricePhase? get() = introPhases.firstOrNull { it.isFree }
    val introPrice: PricePhase? get() = introPhases.firstOrNull { !it.isFree }
}

/** Pure helpers for choosing an offer and describing it honestly, in plain words. */
object PlanPricing {

    /**
     * Play only returns offers this user is eligible for. Prefer one with a free trial, then one
     * with an intro price, then the base plan; the result must end in a renewing phase.
     */
    fun pickOffer(offers: List<PlanOffer>): PlanOffer? {
        val valid = offers.filter { it.phases.isNotEmpty() && it.recurring.renewsForever }
        return valid.firstOrNull { it.freeTrial != null }
            ?: valid.firstOrNull { it.introPrice != null }
            ?: valid.firstOrNull { it.introPhases.isEmpty() }
            ?: valid.firstOrNull()
    }

    /** "P1M" → "month", "P1Y" → "year", "P3M" → "3 months", "P1W" → "week". */
    fun periodUnit(iso: String): String {
        val (n, unit) = parse(iso) ?: return iso
        return if (n == 1) unit else "$n ${unit}s"
    }

    /** A span of [cycles] periods: ("P1W", 1) → "1 week", ("P1M", 3) → "3 months". */
    fun duration(iso: String, cycles: Int = 1): String {
        val (n, unit) = parse(iso) ?: return iso
        val total = n * cycles.coerceAtLeast(1)
        return if (total == 1) "1 $unit" else "$total ${unit}s"
    }

    /** "$19.99 / year" for the renewing phase. */
    fun recurringLabel(offer: PlanOffer): String =
        "${offer.recurring.formattedPrice} / ${periodUnit(offer.recurring.billingPeriod)}"

    /**
     * The trial / intro line shown under a plan, or null when the offer has none, e.g.
     * "7-day free trial, then $19.99 / year" or "$0.99 / month for 3 months, then $2.99 / month".
     */
    fun introLabel(offer: PlanOffer): String? {
        val then = "then ${recurringLabel(offer)}"
        val parts = offer.introPhases.map { phase ->
            val span = duration(phase.billingPeriod, phase.cycles)
            when {
                phase.isFree -> "Free for $span"
                phase.cycles > 1 -> "${phase.formattedPrice} / ${periodUnit(phase.billingPeriod)} for $span"
                else -> "${phase.formattedPrice} for the first $span"
            }
        }
        return if (parts.isEmpty()) null else parts.joinToString(", ") + ", $then"
    }

    private fun parse(iso: String): Pair<Int, String>? {
        val m = Regex("""^P(\d+)([DWMY])$""").find(iso.uppercase()) ?: return null
        val n = m.groupValues[1].toIntOrNull() ?: return null
        val unit = when (m.groupValues[2]) {
            "D" -> "day"
            "W" -> "week"
            "M" -> "month"
            else -> "year"
        }
        return n to unit
    }
}
