package com.cy.loxia.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cy.loxia.MainViewModel
import com.cy.loxia.R
import com.cy.loxia.Wardrobe
import com.cy.loxia.WardrobeAdapter
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    interface Host {
        fun onWardrobeClick(wardrobe: Wardrobe)
        fun onWardrobeLongPress(wardrobe: Wardrobe, position: Int)
        fun onAddWardrobeClick()
        fun onTotalCostClick()
    }

    private var host: Host? = null
    private lateinit var viewModel: MainViewModel

    private lateinit var rvWardrobes: RecyclerView
    private lateinit var wardrobeAdapter: WardrobeAdapter
    private var tvTotalCount: TextView? = null
    private var tvTotalCost: TextView? = null
    private var bannerFinalPayment: View? = null
    private var tvBannerMessage: TextView? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Host) host = context
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.page_my_wardrobes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        tvTotalCost = view.findViewById(R.id.tvTotalCost)
        bannerFinalPayment = view.findViewById(R.id.bannerFinalPayment)
        tvBannerMessage = view.findViewById(R.id.tvBannerMessage)
        val ivBannerClose = view.findViewById<ImageView>(R.id.ivBannerClose)

        // Wardrobe list
        rvWardrobes = view.findViewById(R.id.rvWardrobes)
        wardrobeAdapter = WardrobeAdapter { wardrobe -> host?.onWardrobeClick(wardrobe) }
        wardrobeAdapter.setLongPressListener { wardrobe, position -> host?.onWardrobeLongPress(wardrobe, position) }
        rvWardrobes.apply {
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
            adapter = wardrobeAdapter
            setItemViewCacheSize(10)
        }

        // Add wardrobe button
        view.findViewById<MaterialButton>(R.id.btnAddWardrobe).setOnClickListener {
            host?.onAddWardrobeClick()
        }

        // Total cost click — always allow; the observer handles display state
        val costSection = tvTotalCost?.parent as? View
        costSection?.isClickable = true
        costSection?.isFocusable = true
        costSection?.setOnClickListener {
            host?.onTotalCostClick()
        }

        // Banner close
        ivBannerClose?.setOnClickListener {
            viewModel.markBannerAsActioned()
            hideBanner()
        }

        // Observe wardrobe list (ViewModel 已转换为 Domain Model)
        viewModel.wardrobeList.observe(viewLifecycleOwner) { wardrobes ->
            if (wardrobes == null) return@observe
            wardrobeAdapter.submitList(wardrobes)
        }

        // Observe stats
        viewModel.totalDressCount.observe(viewLifecycleOwner) { count ->
            if (count != null) tvTotalCount?.text = count.toString()
        }
        // Observe cost AND hidden state — use syncCostDisplay for consistent rendering
        viewModel.totalCost.observe(viewLifecycleOwner) { _ ->
            syncCostDisplay()
        }
        viewModel.isTotalCostHidden.observe(viewLifecycleOwner) { _ ->
            syncCostDisplay()
        }

        // Observe banner
        viewModel.showHomeBanner.observe(viewLifecycleOwner) { show ->
            if (show == true && !viewModel.hasBannerBeenActioned()) {
                showBanner()
            } else {
                hideBanner()
            }
        }
        viewModel.urgentFinalPaymentList.observe(viewLifecycleOwner) { list ->
            if (list != null && list.isNotEmpty()) {
                tvBannerMessage?.text = String.format("\u4f60\u6709 %d \u4ef6\u88d9\u5b50\u5f85\u8865\u5c3e\u6b3e\uff0c\u53ca\u65f6\u5904\u7406\u54e6~", list.size)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Explicitly sync cost display from ViewModel when returning to this fragment
        // This handles the case where cost was revealed from another tab (Profile)
        syncCostDisplay()
    }

    /**
     * Sync the total cost display with ViewModel state.
     * Called from onResume and after observers fire, to handle cross-tab state changes.
     */
    private fun syncCostDisplay() {
        val hidden = viewModel.isTotalCostHidden.value != false
        val cost = viewModel.totalCost.value
        tvTotalCost?.text = if (hidden || cost == null) "****" else String.format("\u00a5%.2f", cost)
    }

    override fun onDetach() {
        host = null
        super.onDetach()
    }

    fun setTotalCostRevealed(cost: Double) {
        viewModel.setTotalCostHidden(false)
        syncCostDisplay()
    }

    private fun showBanner() {
        bannerFinalPayment?.let { banner ->
            banner.alpha = 1f  // 重置 alpha（XML 初始值为 0）
            banner.visibility = View.VISIBLE
            val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300 }
            banner.startAnimation(fadeIn)
        }
    }

    private fun hideBanner() {
        bannerFinalPayment?.let { banner ->
            // 如果 banner 当前不可见（如首次加载），直接 GONE，无需动画
            if (banner.visibility != View.VISIBLE) {
                banner.visibility = View.GONE
                return
            }
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 200
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(a: Animation?) {}
                    override fun onAnimationRepeat(a: Animation?) {}
                    override fun onAnimationEnd(a: Animation?) { banner.visibility = View.GONE }
                })
            }
            banner.startAnimation(fadeOut)
        }
    }
}