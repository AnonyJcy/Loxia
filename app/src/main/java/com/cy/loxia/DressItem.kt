package com.cy.loxia

import org.json.JSONException
import org.json.JSONObject

data class DressItem @JvmOverloads constructor(
    val id: String,
    var wardrobeId: String,
    val name: String,
    val store: String,
    val channel: String,
    val price: Double,
    val buyDate: String,
    var status: String,
    val remindAt: String,
    val remark: String,
    var earnestMoney: Double = 0.0,
    var isFullPayment: Boolean = false,
    var fullPaymentAmount: Double = 0.0,
    var tailPayment: Double = 0.0,
    var imageUri: String = "",
    var shippingFee: String = "包邮",
    var deposit: Double = 0.0,
    var pinned: Boolean = false,
    var sortOrder: Int = 0,
    var daiqiangDate: String = "",
    var yixiangDate: String = "",
    var dingjinDate: String = "",
    var buweikuanDate: String = "",
    var isWishlist: Boolean = false,
    var addTime: Long = 0L,
    var shipmentDate: String = "",
    var receivedDate: String = "",
    var expectedShipmentDate: String = ""
) {
    // 为了兼容性，保留 getter/setter
    fun getTailPaymentDate(): String = buweikuanDate

    fun setTailPaymentDate(date: String) {
        buweikuanDate = date
    }

    // Java 兼容性方法
    fun isPinned(): Boolean = pinned

    fun getEffectiveTotal(): Double {
        var total = 0.0
        // 意向金（两种状态都有）
        total += earnestMoney
        if (isFullPayment) {
            // 状态一：意向金 + 全款 + 运费
            total += fullPaymentAmount
        } else {
            // 状态二：意向金 + 定金 + 尾款 + 运费
            total += deposit
            total += tailPayment
        }
        // 运费
        total += parseShippingFee()
        // 如果没有填写任何价格，使用 price 字段
        if (total < 0.001) {
            total = price
        }
        return total
    }

    private fun parseShippingFee(): Double {
        if (shippingFee.isEmpty() || shippingFee == "包邮") return 0.0
        return try {
            val cleaned = shippingFee
                .replace("¥", "")
                .replace("$", "")
                .replace("元", "")
                .trim()
            cleaned.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    @Throws(JSONException::class)
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("wardrobeId", wardrobeId)
            put("name", name)
            put("store", store)
            put("channel", channel)
            put("price", price)
            put("buyDate", buyDate)
            put("status", status)
            put("remindAt", remindAt)
            put("remark", remark)
            put("earnestMoney", earnestMoney)
            put("isFullPayment", isFullPayment)
            put("fullPaymentAmount", fullPaymentAmount)
            put("tailPayment", tailPayment)
            put("tailPaymentDate", getTailPaymentDate())
            put("imageUri", imageUri)
            put("shippingFee", shippingFee)
            put("deposit", deposit)
            put("pinned", pinned)
            put("sortOrder", sortOrder)
            put("daiqiangDate", daiqiangDate)
            put("yixiangDate", yixiangDate)
            put("dingjinDate", dingjinDate)
            put("buweikuanDate", buweikuanDate)
            put("shipmentDate", shipmentDate)
            put("receivedDate", receivedDate)
            put("expectedShipmentDate", expectedShipmentDate)
        }
    }

    companion object {
        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(json: JSONObject): DressItem {
            val item = DressItem(
                id = json.optString("id", ""),
                wardrobeId = json.optString("wardrobeId", ""),
                name = json.optString("name", ""),
                store = json.optString("store", ""),
                channel = json.optString("channel", ""),
                price = json.optDouble("price", 0.0),
                buyDate = json.optString("buyDate", ""),
                status = json.optString("status", ""),
                remindAt = json.optString("remindAt", ""),
                remark = json.optString("remark", "")
            )

            item.earnestMoney = json.optDouble("earnestMoney", 0.0)
            item.isFullPayment = json.optBoolean("isFullPayment", false)
            item.fullPaymentAmount = json.optDouble("fullPaymentAmount", 0.0)
            item.tailPayment = json.optDouble("tailPayment", 0.0)
            item.imageUri = json.optString("imageUri", "")
            item.shippingFee = json.optString("shippingFee", "包邮")
            item.deposit = json.optDouble("deposit", 0.0)
            item.pinned = json.optBoolean("pinned", false)
            item.sortOrder = json.optInt("sortOrder", 0)
            item.daiqiangDate = json.optString("daiqiangDate", "")
            item.yixiangDate = json.optString("yixiangDate", "")
            item.dingjinDate = json.optString("dingjinDate", "")

            // buweikuanDate（兼容旧数据中的 tailPaymentDate 字段名）
            val bwkDate = json.optString("buweikuanDate", "")
            if (bwkDate.isNotEmpty()) {
                item.buweikuanDate = bwkDate
            } else {
                val tpd = json.optString("tailPaymentDate", "")
                if (tpd.isNotEmpty()) {
                    item.buweikuanDate = tpd
                }
            }

            item.isWishlist = json.optBoolean("isWishlist", false)
            item.addTime = json.optLong("addTime", 0L)
            item.shipmentDate = json.optString("shipmentDate", "")
            item.receivedDate = json.optString("receivedDate", "")
            item.expectedShipmentDate = json.optString("expectedShipmentDate", "")

            return item
        }
    }
}
