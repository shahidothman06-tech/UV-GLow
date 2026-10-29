package com.tubers.blacklight.domain

import com.tubers.blacklight.domain.model.AppLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguagesTest {

    @Test fun preselectsTheDeviceLanguageWhenOffered() {
        assertEquals("es", AppLanguages.preselect("es"))
        assertEquals("ur", AppLanguages.preselect("ur"))
    }

    @Test fun legacyIndonesianCodeMapsToId() {
        assertEquals("id", AppLanguages.preselect("in"))
    }

    @Test fun unknownDeviceLanguageFallsBackToEnglish() {
        assertEquals(AppLanguages.DEFAULT_TAG, AppLanguages.preselect("xx"))
    }

    @Test fun searchMatchesNativeAndEnglishNames() {
        assertEquals(listOf("de"), AppLanguages.search("deutsch").map { it.tag })
        assertEquals(listOf("de"), AppLanguages.search("German").map { it.tag })
        assertEquals(AppLanguages.all, AppLanguages.search("  "))
        assertTrue(AppLanguages.search("zzz").isEmpty())
    }

    @Test fun tagsAreUnique() {
        assertEquals(AppLanguages.all.size, AppLanguages.all.map { it.tag }.toSet().size)
    }
}
