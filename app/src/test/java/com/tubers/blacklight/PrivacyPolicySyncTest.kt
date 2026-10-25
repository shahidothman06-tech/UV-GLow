package com.tubers.blacklight

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The in-app policy (strings.xml) and the public copy for Google Play (docs/privacy-policy.md)
 * must say the same thing. Fails when one is edited without the other.
 */
class PrivacyPolicySyncTest {

    // Unit tests run with the :app module as the working directory.
    private val strings = File("src/main/res/values/strings.xml").readText()
    private val markdown = File("../docs/privacy-policy.md").readText().normalize()

    private fun String.normalize() = replace(Regex("\\s+"), " ").trim()

    private val privacyStrings: Map<String, String> =
        Regex("""<string name="(privacy_[a-z_]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(strings)
            .associate { m -> m.groupValues[1] to m.groupValues[2].replace("\\'", "'").replace("&amp;", "&").normalize() }

    @Test fun everyPolicyStringAppearsInTheMarkdown() {
        assertTrue("no privacy_* strings found", privacyStrings.size >= 18)
        privacyStrings
            .filterKeys { it != "privacy_title" && it != "privacy_last_updated" }
            .forEach { (name, text) ->
                val expected = text.replace("%1\$s", AppLinks.CONTACT_EMAIL)
                assertTrue("$name is missing from docs/privacy-policy.md: \"$expected\"", expected in markdown)
            }
    }

    @Test fun titleMatches() {
        val title = privacyStrings.getValue("privacy_title")
        assertTrue("docs/privacy-policy.md should start with \"# $title\"", markdown.startsWith("# $title"))
    }

    @Test fun lastUpdatedDateMatches() {
        val date = privacyStrings.getValue("privacy_last_updated").removePrefix("Last updated: ")
        assertTrue("docs/privacy-policy.md has a different date than strings.xml ($date)", "Last updated: $date" in markdown)
    }
}
