package com.cy.loxia

/**
 * 收藏事件：从 DressItem 的时间节点聚合而来
 * 用于日程中心展示待办事项
 */
data class CollectionEvent(
    val id: String,
    val dressId: String,
    val dressName: String,
    val dressImageUri: String,
    val eventType: EventType,
    val eventDate: String,  // yyyy-MM-dd
    val amount: Double = 0.0,
    val remark: String = ""
) {
    /**
     * 计算距离今天的天数
     * 正数 = 未来，负数 = 已过
     */
    fun daysFromToday(): Int {
        return try {
            val eventLocalDate = java.time.LocalDate.parse(eventDate)
            val today = java.time.LocalDate.now()
            java.time.temporal.ChronoUnit.DAYS.between(today, eventLocalDate).toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 获取剩余天数描述
     */
    fun getRemainingText(): String {
        val days = daysFromToday()
        return when {
            days < 0 -> "已过${-days}天"
            days == 0 -> "今天"
            days == 1 -> "明天"
            days <= 7 -> "剩余${days}天"
            else -> "剩余${days}天"
        }
    }
}

/**
 * 事件类型枚举
 */
enum class EventType(val label: String, val icon: String) {
    DAI_QIANG("待抢", "抢"),
    YI_XIANG("付意向", "意"),
    DING_JIN("付定金", "定"),
    BU_WEI_KUAN("补尾款", "尾"),
    EXPECTED_SHIPMENT("预计发货", "预"),
    SHIPMENT("发货", "发"),
    RECEIVED("确认收货", "收"),
    CUSTOM("自定义", "自");

    companion object {
        /**
         * 从状态字符串推断事件类型
         */
        fun fromStatus(status: String): List<EventType> {
            val types = mutableListOf<EventType>()
            if (status.contains("待抢")) types.add(DAI_QIANG)
            if (status.contains("付意向")) types.add(YI_XIANG)
            if (status.contains("付定金")) types.add(DING_JIN)
            if (status.contains("补尾款")) types.add(BU_WEI_KUAN)
            if (status.contains("待发货")) types.add(EXPECTED_SHIPMENT)
            if (status.contains("已到手")) types.add(RECEIVED)
            return types
        }
    }
}
