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

        // Total cost click
        val costSection = tvTotalCost?.parent as? View
        costSection?.isClickable = true
        costSection?.isFocusable = true
        costSection?.setOnClickListener {
            if (viewModel.isTotalCostHidden.value != false) {
                host?.onTotalCostClick()
            }
        }

        // Banner close
        ivBannerClose?.setOnClickListener {
            viewModel.markBannerAsActioned()
            hideBanner()
        }

        // Observe wardrobe list (ViewModel 已转换为 Domain Model)
        viewModel.wardrobeList.observe(viewLifecycleOwner) { wardrobes ->
            if (wardrobes == null) return@observe
            val isInitialLoad = wardrobeAdapter.itemCount == 0 && wardrobes.isNotEmpty()
            if (isInitialLoad) {
                rvWardrobes.layoutAnimation = android.view.animation.AnimationUtils.loadLayoutAnimation(context, R.anim.layout_anim_slide_up)
            }
            wardrobeAdapter.submitList(wardrobes) {
                if (isInitialLoad) {
                    rvWardrobes.scheduleLayoutAnimation()
                }
            }
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
            banner.alpha = 0f
            banner.scaleX = 0.95f
            banner.scaleY = 0.95f
            banner.visibility = View.VISIBLE
            banner.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
                .start()
        }
    }

    private fun hideBanner() {
        bannerFinalPayment?.let { banner ->
            if (banner.visibility != View.VISIBLE) {
                banner.visibility = View.GONE
                return
            }
            banner.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(200)
                .setInterpolator(android.view.animation.AccelerateInterpolator(1.5f))
                .withEndAction { banner.visibility = View.GONE }
                .start()
        }
    }
}