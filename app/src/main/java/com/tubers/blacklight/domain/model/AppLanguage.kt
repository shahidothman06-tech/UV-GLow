package com.tubers.blacklight.domain.model

/** A language offered on the language screen. [tag] is a BCP 47 language tag. */
data class AppLanguage(val tag: String, val nativeName: String, val englishName: String)

object AppLanguages {
    const val DEFAULT_TAG = "en"

    val all: List<AppLanguage> = listOf(
        AppLanguage("en", "English", "English"),
        AppLanguage("es", "Español", "Spanish"),
        AppLanguage("pt", "Português", "Portuguese"),
        AppLanguage("fr", "Français", "French"),
        AppLanguage("de", "Deutsch", "German"),
        AppLanguage("hi", "हिन्दी", "Hindi"),
        AppLanguage("ur", "اردو", "Urdu"),
        AppLanguage("ar", "العربية", "Arabic"),
        AppLanguage("id", "Bahasa Indonesia", "Indonesian"),
        AppLanguage("tr", "Türkçe", "Turkish"),
        AppLanguage("ru", "Русский", "Russian"),
        AppLanguage("ja", "日本語", "Japanese"),
    )

    fun find(tag: String?): AppLanguage? = all.firstOrNull { it.tag == tag }

    /** The device's language when it's on the list, else English. */
    fun preselect(deviceLanguage: String): String {
        // Older Android versions report Indonesian with its legacy code "in".
        val lang = if (deviceLanguage == "in") "id" else deviceLanguage
        return find(lang)?.tag ?: DEFAULT_TAG
    }

    /** Languages whose native or English name, or tag, contains [query] (case-insensitive). */
    fun search(query: String): List<AppLanguage> {
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter {
            it.nativeName.contains(q, ignoreCase = true) ||
                it.englishName.contains(q, ignoreCase = true) ||
                it.tag.equals(q, ignoreCase = true)
        }
    }
}
