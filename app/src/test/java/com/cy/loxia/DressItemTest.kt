package com.cy.loxia

import org.junit.Assert.*
import org.junit.Test

class DressItemTest {

    @Test
    fun `test getEffectiveTotal with full payment`() {
        val item = DressItem(
            id = "test-id",
            wardrobeId = "wardrobe-1",
            name = "Test Dress",
            store = "Test Store",
            channel = "淘宝",
            price = 100.0,
            buyDate = "2026-01-01",
            status = "已到手",
            remindAt = "",
            remark = ""
        ).apply {
            isFullPayment = true
            fullPaymentAmount = 90.0
        }

        assertEquals(90.0, item.getEffectiveTotal(), 0.01)
    }

    @Test
    fun `test getEffectiveTotal with earnest and tail payment`() {
        val item = DressItem(
            id = "test-id",
            wardrobeId = "wardrobe-1",
            name = "Test Dress",
            store = "Test Store",
            channel = "淘宝",
            price = 100.0,
            buyDate = "2026-01-01",
            status = "已到手",
            remindAt = "",
            remark = ""
        ).apply {
            earnestMoney = 30.0
            tailPayment = 70.0
        }

        assertEquals(100.0, item.getEffectiveTotal(), 0.01)
    }

    @Test
    fun `test getEffectiveTotal with shipping fee`() {
        val item = DressItem(
            id = "test-id",
            wardrobeId = "wardrobe-1",
            name = "Test Dress",
            store = "Test Store",
            channel = "淘宝",
            price = 100.0,
            buyDate = "2026-01-01",
            status = "已到手",
            remindAt = "",
            remark = ""
        ).apply {
            shippingFee = "¥10"
        }

        assertEquals(10.0, item.getEffectiveTotal(), 0.01)
    }

    @Test
    fun `test getEffectiveTotal with deposit`() {
        val item = DressItem(
            id = "test-id",
            wardrobeId = "wardrobe-1",
            name = "Test Dress",
            store = "Test Store",
            channel = "淘宝",
            price = 100.0,
            buyDate = "2026-01-01",
            status = "已到手",
            remindAt = "",
            remark = ""
        ).apply {
            deposit = 5.0
        }

        assertEquals(5.0, item.getEffectiveTotal(), 0.01)
    }

    @Test
    fun `test isPinned method`() {
        val item = DressItem(
            id = "test-id",
            wardrobeId = "wardrobe-1",
            name = "Test Dress",
            store = "Test Store",
            channel = "淘宝",
            price = 100.0,
            buyDate = "2026-01-01",
            status = "已到手",
            remindAt = "",
            remark = ""
        )

        assertFalse(item.isPinned())

        val pinnedItem = item.copy(pinned = true)
        assertTrue(pinnedItem.isPinned())
    }

    @Test
    fun `test getTailPaymentDate with buweikuanDate`() {
        val item = DressItem(
            id = "test-id",
            wardrobeId = "wardrobe-1",
            name = "Test Dress",
            store = "Test Store",
            channel = "淘宝",
            price = 100.0,
            buyDate = "2026-01-01",
            status = "已到手",
            remindAt = "",
            remark = "",
            buweikuanDate = "2026-02-01"
        )

        assertEquals("2026-02-01", item.getTailPaymentDate())
    }

    @Test
    fun `test copy with new values`() {
        val item = DressItem(
            id = "test-id",
            wardrobeId = "wardrobe-1",
            name = "Test Dress",
            store = "Test Store",
            channel = "淘宝",
            price = 100.0,
            buyDate = "2026-01-01",
            status = "已到手",
            remindAt = "",
            remark = ""
        )

        val updated = item.copy(name = "Updated Dress", price = 150.0)
        assertEquals("Updated Dress", updated.name)
        assertEquals(150.0, updated.price, 0.01)
        assertEquals(item.id, updated.id)
    }
}
