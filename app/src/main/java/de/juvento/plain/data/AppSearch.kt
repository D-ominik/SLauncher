package de.juvento.plain.data

import java.text.Normalizer

/** Matching of the app drawer search field against app names and keywords. */
object AppSearch {

    private val combiningMarks = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val separators = Regex("[-_+,. ]")

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .replace(separators, "")
            .lowercase()

    /** True if [query] occurs in [text], ignoring case, accents and separators like '-' or ' '. */
    fun matches(text: String, query: String): Boolean {
        if (text.contains(query, ignoreCase = true)) return true
        val normalizedQuery = normalize(query)
        return normalizedQuery.isNotEmpty() && normalize(text).contains(normalizedQuery)
    }

    /** True if the query matches any of the [searchTerms] (name, original label, keywords). */
    fun matchesAny(searchTerms: List<String>, query: String): Boolean =
        query.isEmpty() || searchTerms.any { matches(it, query) }

    fun matches(app: AppModel, query: String): Boolean =
        matchesAny(listOf(app.name, app.appLabel) + app.keywords, query)
}
