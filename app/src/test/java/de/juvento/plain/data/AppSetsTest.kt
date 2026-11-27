package de.juvento.plain.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSetsTest {

    private val work = AppSet(
        "1", "Work",
        listOf(
            HomeApp("com.mail", "com.mail.Main", "UserHandle{0}", "Mail"),
            HomeApp("com.android.chrome", "", "UserHandle{0}", "web.dev", shortcutId = "abc"),
        )
    )
    private val free = AppSet("2", "Free time", emptyList())

    @Test
    fun sanitizeKeepsOnlyAllowedCharactersAndLimitsLength() {
        assertEquals("Büro 2_a-b.c", AppSets.sanitizeInput("Büro 2_a-b.c!?/\\\n"))
        assertEquals(AppSets.NAME_MAX_LENGTH, AppSets.sanitizeInput("x".repeat(100)).length)
    }

    @Test
    fun validateRejectsEmptyAndDuplicateNames() {
        val sets = listOf(work, free)
        assertEquals(SetNameError.Empty, AppSets.validateName("  ", sets, null))
        assertEquals(SetNameError.Empty, AppSets.validateName("!!!", sets, null))
        assertEquals(SetNameError.Duplicate, AppSets.validateName("work", sets, null))
        assertNull("a set may keep its own name", AppSets.validateName("Work", sets, "1"))
        assertNull(AppSets.validateName("Sport", sets, null))
    }

    @Test
    fun findByNameIgnoresCaseAndSurroundingSpace() {
        assertEquals(free, AppSets.findByName(listOf(work, free), "  FREE TIME "))
        assertNull(AppSets.findByName(listOf(work, free), "unknown"))
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val sets = listOf(work, free)
        assertEquals(sets, AppSets.decode(AppSets.encode(sets)))
    }

    @Test
    fun shortcutsOfOnePackageHaveDistinctIdentities() {
        val a = HomeApp("com.android.chrome", "", "UserHandle{0}", "A", shortcutId = "1")
        val b = a.copy(shortcutId = "2")
        val app = a.copy(shortcutId = "")
        assertEquals(3, setOf(a.identity, b.identity, app.identity).size)
    }

    @Test
    fun decodeOfBrokenDataIsEmpty() {
        assertTrue(AppSets.decode(null).isEmpty())
        assertTrue(AppSets.decode("{not json").isEmpty())
        assertTrue(AppSets.decode("[{\"id\":\"1\"}]").isEmpty())
    }
}
