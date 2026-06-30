package com.cy.loxia

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 月份详情页面
 * 展示指定月份的所有收藏事件
 */
class MonthScheduleActivity : AppCompatActivity() {

    private lateinit var toolbar: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var rvEvents: RecyclerView
    private lateinit var llEmpty: LinearLayout
    private lateinit var ivBack: ImageView

    private lateinit var eventAdapter: CollectionEventAdapter
    private lateinit var repository: DataRepository

    private var year: Int = 0
    private var month: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // EdgeToEdge 适配
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_month_schedule)

        // 获取参数
        year = intent.getIntExtra("year", 2026)
        month = intent.getIntExtra("month", 1)

        // 初始化
        repository = DataRepository.getInstance(this)
        initViews()
        setupWindowInsets()
        loadData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvTitle = findViewById(R.id.tvTitle)
        rvEvents = findViewById(R.id.rvEvents)
        llEmpty = findViewById(R.id.llEmpty)
        ivBack = findViewById(R.id.ivBack)

        // 设置标题
        tvTitle.text = "${year}年${month}月"

        // 返回按钮
        ivBack.setOnClickListener { finish() }

        // 设置列表
        eventAdapter = CollectionEventAdapter { event ->
            // 点击事件跳转到裙子详情
            val intent = android.content.Intent().apply {
                putExtra("dressId", event.dressId)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
        rvEvents.apply {
            layoutManager = LinearLayoutManager(this@MonthScheduleActivity)
            adapter = eventAdapter
        }
    }

    /**
     * 设置 WindowInsets 适配
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(rvEvents) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom + view.paddingBottom
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(llEmpty) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom + view.paddingBottom
            )
            insets
        }
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val items = repository.getDressItemsAsync()
                val allEvents = EventAggregator.aggregateEvents(items)
                val monthEvents = EventAggregator.getEventsByMonth(allEvents, year, month)

                withContext(Dispatchers.Main) {
                    if (monthEvents.isEmpty()) {
                        rvEvents.visibility = View.GONE
                        llEmpty.visibility = View.VISIBLE
                    } else {
                        rvEvents.visibility = View.VISIBLE
                        llEmpty.visibility = View.GONE
                        eventAdapter.submitList(monthEvents)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    rvEvents.visibility = View.GONE
                    llEmpty.visibility = View.VISIBLE
                }
            }
        }
    }
}
