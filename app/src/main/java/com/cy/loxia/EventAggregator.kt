package com.cy.loxia

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 事件聚合器：从 DressItem 列表聚合出 CollectionEvent 列表
 * 用于日程中心的数据计算
 */
object EventAggregator {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 从裙子列表聚合所有事件
     */
    fun aggregateEvents(items: List<DressItem>): List<CollectionEvent> {
        val events = mutableListOf<CollectionEvent>()

        for (item in items) {
            // 待抢时间
            if (item.daiqiangDate.isNotEmpty()) {
                events.add(createEvent(item, EventType.DAI_QIANG, item.daiqiangDate))
            }

            // 付意向时间
            if (item.yixiangDate.isNotEmpty()) {
                events.add(createEvent(item, EventType.YI_XIANG, item.yixiangDate, item.earnestMoney))
            }

            // 付定金时间
            if (item.dingjinDate.isNotEmpty()) {
                events.add(createEvent(item, EventType.DING_JIN, item.dingjinDate, item.deposit))
            }

            // 补尾款时间
            if (item.buweikuanDate.isNotEmpty()) {
                events.add(createEvent(item, EventType.BU_WEI_KUAN, item.buweikuanDate, item.tailPayment))
            }

            // 预计发货时间
            if (item.expectedShipmentDate.isNotEmpty()) {
                events.add(createEvent(item, EventType.EXPECTED_SHIPMENT, item.expectedShipmentDate))
            }

            // 实际发货时间
            if (item.shipmentDate.isNotEmpty()) {
                events.add(createEvent(item, EventType.SHIPMENT, item.shipmentDate))
            }

            // 确认收货时间
            if (item.receivedDate.isNotEmpty()) {
                events.add(createEvent(item, EventType.RECEIVED, item.receivedDate))
            }
        }

        return events.sortedBy { it.eventDate }
    }

    /**
     * 获取未来 N 天内的事件
     */
    fun getUpcomingEvents(events: List<CollectionEvent>, days: Int): List<CollectionEvent> {
        val today = LocalDate.now(zone)
        val endDate = today.plusDays(days.toLong())

        return events.filter { event ->
            try {
                val eventDate = LocalDate.parse(event.eventDate, dateFormatter)
                !eventDate.isBefore(today) && !eventDate.isAfter(endDate)
            } catch (e: Exception) {
                false
            }
        }.sortedBy { it.eventDate }
    }

    /**
     * 获取指定月份的事件
     */
    fun getEventsByMonth(events: List<CollectionEvent>, year: Int, month: Int): List<CollectionEvent> {
        // Note: year/month filtering uses parsed LocalDate, no zone dependency
        return events.filter { event ->
            try {
                val eventDate = LocalDate.parse(event.eventDate, dateFormatter)
                eventDate.year == year && eventDate.monthValue == month
            } catch (e: Exception) {
                false
            }
        }.sortedBy { it.eventDate }
    }

    /**
     * 按月份分组统计
     */
    fun getMonthStatistics(events: List<CollectionEvent>, year: Int): Map<Int, MonthStats> {
        val stats = mutableMapOf<Int, MonthStats>()

        for (month in 1..12) {
            val monthEvents = getEventsByMonth(events, year, month)
            val pendingAmount = monthEvents
                .filter { it.eventType != EventType.RECEIVED }
                .sumOf { it.amount }

            stats[month] = MonthStats(
                month = month,
                eventCount = monthEvents.size,
                pendingAmount = pendingAmount
            )
        }

        return stats
    }

    /**
     * 按事件类型统计
     */
    fun getEventTypeStatistics(events: List<CollectionEvent>): Map<EventType, Int> {
        return events.groupBy { it.eventType }
            .mapValues { it.value.size }
    }

    /**
     * 计算本月待支付金额
     */
    fun getThisMonthPendingAmount(events: List<CollectionEvent>): Double {
        val today = LocalDate.now(zone)
        val thisMonth = YearMonth.now(zone)

        return events.filter { event ->
            try {
                val eventDate = LocalDate.parse(event.eventDate, dateFormatter)
                eventDate.year == thisMonth.year &&
                eventDate.monthValue == thisMonth.monthValue &&
                !eventDate.isBefore(today) &&
                event.amount > 0
            } catch (e: Exception) {
                false
            }
        }.sumOf { it.amount }
    }

    private fun createEvent(
        item: DressItem,
        eventType: EventType,
        eventDate: String,
        amount: Double = 0.0
    ): CollectionEvent {
        return CollectionEvent(
            id = "${item.id}_${eventType.name}_$eventDate",
            dressId = item.id,
            dressName = item.name,
            dressImageUri = item.imageUri,
            eventType = eventType,
            eventDate = eventDate,
            amount = amount
        )
    }

    /**
     * 月度统计数据
     */
    data class MonthStats(
        val month: Int,
        val eventCount: Int,
        val pendingAmount: Double
    )
}
