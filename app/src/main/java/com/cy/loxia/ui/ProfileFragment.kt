package com.cy.loxia.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cy.loxia.DataRepository
import com.cy.loxia.ImageUtils
import com.cy.loxia.MainViewModel
import com.cy.loxia.R
import java.time.LocalDate

/**
 * Profile tab Fragment: user info, statistics summary, settings navigation.
 *
 * Owns all view bindings and lifecycle-aware data observation.
 * Replaces ProfilePageManager to establish true Single-Activity Architecture.
 */
class ProfileFragment : Fragment() {

    interface Host {
        fun onProfileNavigateToThemeSettings()
        fun onProfileNavigateToStats()
        fun onProfileNavigateToDataImport()
        fun onProfileNavigateToNotifications()
        fun onProfileNavigateToProfileDetail()
    }

    private var host: Host? = null

    private lateinit var viewModel: MainViewModel
    private lateinit var repository: DataRepository

    private var tvProfileTotalCount: TextView? = null
    private var tvProfileTotalCost: TextView? = null
    private var tvProfileMonthCount: TextView? = null
    private var ivProfileAvatarSmall: ImageView? = null
    private var tvProfileName: TextView? = null
    private var tvProfileSlogan: TextView? = null
    private var isCostHidden = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Host) {
            host = context
        }
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        repository = DataRepository.getInstance(context.applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvProfileTotalCount = view.findViewById(R.id.tvProfileTotalCount)
        tvProfileTotalCost = view.findViewById(R.id.tvProfileTotalCost)
        tvProfileMonthCount = view.findViewById(R.id.tvProfileMonthCount)
        ivProfileAvatarSmall = view.findViewById(R.id.ivProfileAvatarSmall)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileSlogan = view.findViewById(R.id.tvProfileSlogan)

        view.findViewById<View>(R.id.tvThemeSettings)?.setOnClickListener {
            host?.onProfileNavigateToThemeSettings()
        }
        view.findViewById<View>(R.id.tvStatsCenter)?.setOnClickListener {
            host?.onProfileNavigateToStats()
        }
        view.findViewById<View>(R.id.tvDataBackup)?.setOnClickListener {
            host?.onProfileNavigateToDataImport()
        }
        view.findViewById<View>(R.id.tvNotifications)?.setOnClickListener {
            host?.onProfileNavigateToNotifications()
        }
        view.findViewById<View>(R.id.cardProfileInfo)?.setOnClickListener {
            host?.onProfileNavigateToProfileDetail()
        }
        view.findViewById<View>(R.id.cardProfileCost)?.setOnClickListener {
            if (isCostHidden) showCostDialog()
        }

        // Observe ViewModel stats
        viewModel.totalDressCount.observe(viewLifecycleOwner) { count ->
            if (count != null) tvProfileTotalCount?.text = count.toString()
        }
        viewModel.totalCost.observe(viewLifecycleOwner) { cost ->
            if (cost != null) {
                tvProfileTotalCost?.text = if (isCostHidden) "****" else String.format("\u00a5%.2f", cost)
            }
        }
        viewModel.thisMonthItemCount.observe(viewLifecycleOwner) { count ->
            if (count != null) tvProfileMonthCount?.text = String.format("+%d", count)
        }
        // Observe shared hidden state (syncs across Home & Profile pages)
        viewModel.isTotalCostHidden.observe(viewLifecycleOwner) { hidden ->
            isCostHidden = hidden != false
            val cost = viewModel.totalCost.value
            tvProfileTotalCost?.text = if (isCostHidden || cost == null) "****" else String.format("\u00a5%.2f", cost)
        }

        updatePanel()
    }

    override fun onResume() {
        super.onResume()
        updatePanel()
    }

    override fun onDetach() {
        host = null
        super.onDetach()
    }

    fun updatePanel() {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("loxia_prefs", Context.MODE_PRIVATE)

        // Avatar
        val avatarUri = prefs.getString("profile_avatar_uri", null)
        ivProfileAvatarSmall?.let {
            ImageUtils.loadIntoView(ctx, avatarUri, it, R.drawable.bg_image_placeholder)
        }

        // Nickname
        val nickname = prefs.getString("profile_nickname", null)
        if (nickname != null) tvProfileName?.text = nickname

        // Slogan
        tvProfileSlogan?.text = getDailySlogan()

        // Sync with ViewModel shared state instead of local-only flag
        isCostHidden = viewModel.isTotalCostHidden.value != false
        tvProfileTotalCost?.text = if (isCostHidden) "****" else {
            val cost = viewModel.totalCost.value ?: 0.0
            String.format("\u00a5%.2f", cost)
        }
    }

    fun getDailySlogan(): String {
        val ctx = context ?: return ""
        val prefs = ctx.getSharedPreferences("loxia_prefs", Context.MODE_PRIVATE)
        val slogans = ctx.resources.getStringArray(R.array.slogan_pool)
        if (slogans.isEmpty()) return ""
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString("slogan_date", null)
        val savedText = prefs.getString("slogan_text", null)
        // 如果缓存的标语不在有效列表中（旧乱码缓存），强制重新选取
        if (savedDate == today && savedText != null && savedText in slogans) {
            return savedText
        }
        val slogan = slogans[(slogans.indices).random()]
        prefs.edit().putString("slogan_date", today).putString("slogan_text", slogan).apply()
        return slogan
    }

    private fun showCostDialog() {
        val ctx = context ?: return
        val dialogManager = com.cy.loxia.ui.DialogManager(ctx, layoutInflater)
        dialogManager.showTotalCostDialog(viewModel.totalCost.value ?: 0.0, object : DialogCallbacks.OnConfirm {
            override fun onConfirm() {
                isCostHidden = false
                viewModel.setTotalCostHidden(false)  // Sync to ViewModel so HomeFragment also sees it
                val cost = viewModel.totalCost.value
                if (cost != null) {
                    tvProfileTotalCost?.text = String.format("\u00a5%.2f", cost)
                }
            }
        })
    }
}
