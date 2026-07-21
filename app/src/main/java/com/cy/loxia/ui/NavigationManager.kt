package com.cy.loxia.ui

import android.view.View
import android.view.ViewStub
import com.cy.loxia.R
import com.google.android.material.appbar.MaterialToolbar

class NavigationManager(
    private val topAppBar: MaterialToolbar?,
    private val containerView: View,
    private val mainContentContainer: View,
    private val bottomNavigationView: View,
    private val pages: MutableMap<String, View>,
    private val onNavigateBack: Runnable
) {
    /** 页面首次 inflate 回调接口 */
    fun interface PageInflateListener {
        fun onPageInflated(pageName: String, pageView: View)
    }

    private var currentPage: String = "page_wardrobes"
    private val pageStack = mutableListOf<String>()
    private val inflatedPages = mutableSetOf<String>()

    /** 页面首次 inflate 后回调，用于初始化子 View 和 listener */
    var onPageInflated: PageInflateListener? = null

    fun hideAllPages() {
        pages.values.forEach { it.visibility = View.GONE }
    }

    private fun resolvePage(pageName: String): View? {
        val entry = pages[pageName]
        if (entry is ViewStub) {
            val inflated = entry.inflate()
            pages[pageName] = inflated
            if (pageName !in inflatedPages) {
                inflatedPages.add(pageName)
                onPageInflated?.onPageInflated(pageName, inflated)
            }
            return inflated
        }
        if (entry != null && pageName !in inflatedPages) {
            inflatedPages.add(pageName)
            onPageInflated?.onPageInflated(pageName, entry)
        }
        return entry
    }

    fun showPage(pageName: String, title: String, showBack: Boolean = false, showBottomNav: Boolean = true) {
        val rootView = containerView.rootView as? android.view.ViewGroup
        if (rootView != null) {
            val transition = android.transition.Fade().apply { duration = 200 }
            android.transition.TransitionManager.beginDelayedTransition(rootView, transition)
        }
        hideAllPages()
        resolvePage(pageName)?.visibility = View.VISIBLE
        containerView.visibility = View.VISIBLE
        mainContentContainer.visibility = View.GONE
        bottomNavigationView.visibility = if (showBottomNav) View.VISIBLE else View.GONE

        topAppBar?.title = title
        if (showBack) {
            topAppBar?.setNavigationIcon(R.drawable.ic_back_arrow)
            topAppBar?.setNavigationOnClickListener { navigateBack() }
        } else {
            topAppBar?.navigationIcon = null
        }

        if (currentPage != pageName) {
            pageStack.add(currentPage)
            currentPage = pageName
        }
    }

    fun navigateBack(): Boolean {
        if (pageStack.isNotEmpty()) {
            val previousPage = pageStack.removeAt(pageStack.size - 1)
            val title = getPageTitle(previousPage)
            val showBottomNav = isMainPage(previousPage)

            val rootView = containerView.rootView as? android.view.ViewGroup
            if (rootView != null) {
                val transition = android.transition.Fade().apply { duration = 200 }
                android.transition.TransitionManager.beginDelayedTransition(rootView, transition)
            }

            hideAllPages()
            if (isMainPage(previousPage)) {
                containerView.visibility = View.GONE
                mainContentContainer.visibility = View.VISIBLE
            } else {
                resolvePage(previousPage)?.visibility = View.VISIBLE
            }
            bottomNavigationView.visibility = if (showBottomNav) View.VISIBLE else View.GONE
            topAppBar?.title = title
            topAppBar?.navigationIcon = null

            currentPage = previousPage
            return true
        }
        return false
    }

    fun isMainPage(pageName: String): Boolean {
        return pageName in listOf("page_wardrobes", "page_overview", "page_profile")
    }

    fun getCurrentPage(): String = currentPage

    private fun getPageTitle(pageName: String): String {
        return when (pageName) {
            "page_wardrobes" -> "主页"
            "page_overview" -> "总览"
            "page_profile" -> "我的"
            "page_detail" -> "衣柜详情"
            "page_add_dress" -> "添加裙子"
            "page_dress_detail" -> "裙子详情"
            "page_profile_detail" -> "个人资料"
            "page_stats" -> "收藏日程"
            "page_theme_settings" -> "主题设置"
            "page_notifications" -> "通知设置"
            "page_data_import" -> "数据导入"
            else -> ""
        }
    }
}
