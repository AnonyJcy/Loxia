package com.cy.loxia

import org.junit.Assert.*
import org.junit.Test

class WardrobeTest {

    @Test
    fun `test copy with new sortOrder`() {
        val wardrobe = Wardrobe(
            id = "test-id",
            name = "Test Wardrobe",
            count = 5,
            updatedAt = 1735689600000L  // 2025-01-01
        )

        val updated = wardrobe.copy(sortOrder = 2)
        assertEquals(2, updated.sortOrder)
        assertEquals(wardrobe.id, updated.id)
        assertEquals(wardrobe.name, updated.name)
    }

    @Test
    fun `test copy with new count`() {
        val wardrobe = Wardrobe(
            id = "test-id",
            name = "Test Wardrobe",
            count = 5,
            updatedAt = 1735689600000L  // 2025-01-01
        )

        val updated = wardrobe.copy(count = 10)
        assertEquals(10, updated.count)
        assertEquals(wardrobe.id, updated.id)
    }

    @Test
    fun `test default values`() {
        val wardrobe = Wardrobe(
            id = "test-id",
            name = "Test Wardrobe",
            count = 0,
            updatedAt = 1735689600000L  // 2025-01-01
        )

        assertFalse(wardrobe.isDemo)
        assertEquals("", wardrobe.cover)
        assertEquals(0, wardrobe.sortOrder)
    }
}
