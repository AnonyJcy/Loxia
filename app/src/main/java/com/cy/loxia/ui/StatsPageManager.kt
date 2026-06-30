package com.cy.loxia.ui

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cy.loxia.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

/**
 * 收藏日程中心页面管理器 V3
 * 负责初始化、数据绑定和交互逻辑
 */
class StatsPageManager(
    private val context: Context,
    private val viewModel: MainViewModel,
    private val repository: DataRepository
) {
    // 保留 Activity context 用于 Dialog，ApplicationContext 用于其他
    private val activityContext: Context = context
    private val appContext: Context = context.applicationContext

    // UI 组件
    private var tvThisMonthPayment: TextView? = null
    private var tvUpcoming30Days: TextView? = null
    private var tvYearTitle: TextView? = null
    private var tvViewMore: TextView? = null
    private var llNoUpcoming: LinearLayout? = null
    private var headerMoreStats: View? = null
    private var tvMoreStatsToggle: TextView? = null

    // RecyclerView
    private lateinit var rvUpcomingEvents: RecyclerView
    private lateinit var rvMonthTimeline: RecyclerView
    private lateinit var rvEventTypeStats: RecyclerView

    // Adapter
    private lateinit var eventAdapter: CollectionEventAdapter
    private lateinit var monthAdapter: MonthCardAdapter
    private lateinit var eventTypeAdapter: EventTypeTagAdapter

    // 状态
    private var isStatsExpanded = false
    private var cachedAllItems: List<DressItem>? = null
    private var cachedAllEvents: List<CollectionEvent>? = null

    /**
     * 初始化页面
     */
    fun initPage(page: View) {
        // 初始化概览卡片
        tvThisMonthPayment = page.findViewById(R.id.tvThisMonthPayment)
        tvUpcoming30Days = page.findViewById(R.id.tvUpcoming30Days)
        tvYearTitle = page.findViewById(R.id.tvYearTitle)
        tvViewMore = page.findViewById(R.id.tvViewMore)
        llNoUpcoming = page.findViewById(R.id.llNoUpcoming)

        // 初始化折叠头部
        headerMoreStats = page.findViewById(R.id.headerMoreStats)
        tvMoreStatsToggle = page.findViewById(R.id.tvMoreStatsToggle)

        // 初始化 RecyclerView
        rvUpcomingEvents = page.findViewById(R.id.rvUpcomingEvents)
        rvMonthTimeline = page.findViewById(R.id.rvMonthTimeline)
        rvEventTypeStats = page.findViewById(R.id.rvEventTypeStats)

        // 设置适配器
        setupUpcomingEventsList()
        setupMonthTimeline()
        setupEventTypeStats()
        setupCollapsibleStats()

        // 设置查看更多点击事件
        tvViewMore?.setOnClickListener {
            // TODO: 跳转到完整事件列表页面
        }
    }

    /**
     * 设置近期事件列表
     */
    private fun setupUpcomingEventsList() {
        eventAdapter = CollectionEventAdapter { event ->
            navigateToDressDetail(event.dressId)
        }
        rvUpcomingEvents.apply {
            layoutManager = LinearLayoutManager(appContext)
            adapter = eventAdapter
            isNestedScrollingEnabled = false
        }
    }

    /**
     * 设置月份时间轴（横向滚动）
     */
    private fun setupMonthTimeline() {
        monthAdapter = MonthCardAdapter { month ->
            val year = LocalDate.now().year
            navigateToMonthSchedule(year, month)
        }
        rvMonthTimeline.apply {
            layoutManager = LinearLayoutManager(appContext, LinearLayoutManager.HORIZONTAL, false)
            adapter = monthAdapter
        }
    }

    /**
     * 设置事件类型统计
     */
    private fun setupEventTypeStats() {
        eventTypeAdapter = EventTypeTagAdapter { eventType ->
            navigateToFilteredEvents(eventType)
        }
        rvEventTypeStats.apply {
            layoutManager = GridLayoutManager(appContext, 2)
            adapter = eventTypeAdapter
            isNestedScrollingEnabled = false
        }
    }

    /**
     * 设置折叠统计
     */
    private fun setupCollapsibleStats() {
        headerMoreStats?.setOnClickListener {
            isStatsExpanded = !isStatsExpanded
            rvEventTypeStats.visibility = if (isStatsExpanded) View.VISIBLE else View.GONE
            tvMoreStatsToggle?.text = if (isStatsExpanded) "▲" else "▼"
        }
    }

    /**
     * 更新页面数据
     */
    fun updatePage() {
        viewModel.fetchStatistics()
        updateOverviewCard()
        loadAllData()
    }

    /**
     * 更新概览卡片
     */
    private fun updateOverviewCard() {
        // 概览卡片只保留本月待支付和未来30天待办
    }

    /**
     * 加载所有数据
     */
    private fun loadAllData() {
        val allItems = cachedAllItems
        if (allItems != null) {
            processData(allItems)
        } else {
            viewModel.viewModelScope.launch {
                val items = viewModel.getAllDressItemsForExport()
                withContext(Dispatchers.Main) {
                    cachedAllItems = items
                    processData(items)
                }
            }
        }
    }

    /**
     * 处理数据并更新 UI
     */
    private fun processData(items: List<DressItem>) {
        val allEvents = EventAggregator.aggregateEvents(items)
        cachedAllEvents = allEvents

        updateOverviewStats(allEvents)
        updateUpcomingEvents(allEvents)
        updateMonthTimeline(allEvents)
        updateEventTypeStats(allEvents)
    }

    /**
     * 更新概览统计数据
     */
    private fun updateOverviewStats(events: List<CollectionEvent>) {
        val thisMonthPayment = EventAggregator.getThisMonthPendingAmount(events)
        tvThisMonthPayment?.text = String.format("¥%.0f", thisMonthPayment)

        val upcoming30Days = EventAggregator.getUpcomingEvents(events, 30)
        tvUpcoming30Days?.text = upcoming30Days.size.toString()
    }

    /**
     * 更新近期事件列表
     */
    private fun updateUpcomingEvents(events: List<CollectionEvent>) {
        val upcomingEvents = EventAggregator.getUpcomingEvents(events, 30)

        if (upcomingEvents.isEmpty()) {
            rvUpcomingEvents.visibility = View.GONE
            llNoUpcoming?.visibility = View.VISIBLE
        } else {
            rvUpcomingEvents.visibility = View.VISIBLE
            llNoUpcoming?.visibility = View.GONE

            val displayEvents = if (upcomingEvents.size > 5) {
                upcomingEvents.take(5)
            } else {
                upcomingEvents
            }
            eventAdapter.submitList(displayEvents)

            tvViewMore?.visibility = if (upcomingEvents.size > 5) View.VISIBLE else View.GONE
        }
    }

    /**
     * 更新月份时间轴（当前月份前后各2个月）
     */
    private fun updateMonthTimeline(events: List<CollectionEvent>) {
        val now = LocalDate.now()
        val currentYear = now.year
        val currentMonth = now.monthValue

        tvYearTitle?.text = currentYear.toString()

        // 生成当前月份前后各2个月（共5个月）
        val months = mutableListOf<Int>()
        for (i in -2..2) {
            val month = currentMonth + i
            if (month in 1..12) {
                months.add(month)
            }
        }

        // 如果月份不足5个，补充到5个
        if (months.size < 5) {
            val startMonth = months.firstOrNull() ?: currentMonth
            for (i in months.size until 5) {
                val m = startMonth - (5 - months.size) + i
                if (m in 1..12 && m !in months) {
                    months.add(m)
                }
            }
            months.sort()
        }

        val monthStats = EventAggregator.getMonthStatistics(events, currentYear)
        val monthCards = months.map { month ->
            val stats = monthStats[month] ?: EventAggregator.MonthStats(month, 0, 0.0)
            MonthCardAdapter.MonthCardData(
                month = stats.month,
                eventCount = stats.eventCount,
                pendingAmount = stats.pendingAmount,
                isCurrentMonth = month == currentMonth
            )
        }
        monthAdapter.submitList(monthCards)

        // 滚动到当前月份
        val currentIndex = months.indexOf(currentMonth)
        if (currentIndex >= 0) {
            rvMonthTimeline.post {
                (rvMonthTimeline.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(currentIndex, 0)
            }
        }
    }

    /**
     * 更新事件类型统计
     */
    private fun updateEventTypeStats(events: List<CollectionEvent>) {
        val typeStats = EventAggregator.getEventTypeStatistics(events)
        val typeCards = EventType.entries.map { type ->
            EventTypeTagAdapter.EventTypeData(
                type = type,
                count = typeStats[type] ?: 0
            )
        }
        eventTypeAdapter.submitList(typeCards)
    }

    /**
     * 设置缓存数据
     */
    fun setCachedItems(items: List<DressItem>) {
        cachedAllItems = items
    }

    /**
     * 跳转到裙子详情
     */
    private fun navigateToDressDetail(dressId: String) {
        if (context is MainActivity) {
            context.navigateToDressDetail(dressId)
        }
    }

    /**
     * 跳转到月份详情
     */
    private fun navigateToMonthSchedule(year: Int, month: Int) {
        val intent = Intent(context, MonthScheduleActivity::class.java).apply {
            putExtra("year", year)
            putExtra("month", month)
        }
        context.startActivity(intent)
    }

    /**
     * 跳转到筛选的事件列表
     */
    private fun navigateToFilteredEvents(eventType: EventType) {
        // TODO: 实现筛选事件列表页面
    }
}
