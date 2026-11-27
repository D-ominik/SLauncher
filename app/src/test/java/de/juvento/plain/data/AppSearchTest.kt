package de.juvento.plain.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    @Test
    fun matchesIgnoringCaseAccentsAndSeparators() {
        assertTrue(AppSearch.matches("Café Maps", "cafe"))
        assertTrue(AppSearch.matches("Google Maps", "googlemaps"))
        assertTrue(AppSearch.matches("Open-Street_Map", "streetm"))
        assertFalse(AppSearch.matches("Calendar", "xyz"))
    }

    @Test
    fun separatorOnlyQueryDoesNotMatchEverything() {
        assertFalse(AppSearch.matches("Calendar", "-"))
    }

    @Test
    fun keywordsAreSearched() {
        assertTrue(AppSearch.matchesAny(listOf("Karte", "Maps", "GPS", "Navigation"), "gps"))
        assertFalse(AppSearch.matchesAny(listOf("Karte", "Maps"), "gps"))
    }

    @Test
    fun emptyQueryMatchesEverything() {
        assertTrue(AppSearch.matchesAny(emptyList(), ""))
    }

    @Test
    fun keywordsAreSplitOnCommasAndLineBreaksWithoutDuplicates() {
        assertEquals(listOf("GPS", "Maps", "karte"), Keywords.parse(" GPS, Maps\nkarte ,gps,, Karte"))
    }
}
