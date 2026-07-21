package com.cy.loxia.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cy.loxia.DressItem
import com.cy.loxia.MainViewModel
import com.cy.loxia.OverviewDressAdapter
import com.cy.loxia.R

class OverviewFragment : Fragment() {

    interface Host {
        fun onDressItemClick(item: DressItem)
    }

    private var host: Host? = null
    private lateinit var viewModel: MainViewModel

    private var rvOverview: RecyclerView? = null
    private var overviewAdapter: OverviewDressAdapter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Host) host = context
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.page_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvOverview = view.findViewById(R.id.rvOverview)
        overviewAdapter = OverviewDressAdapter()
        overviewAdapter?.setOnDressItemClickListener { item -> host?.onDressItemClick(item) }
        rvOverview?.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = overviewAdapter
            setItemViewCacheSize(20)
        }

        // ViewModel 已转换为 Domain Model，直接使用
        viewModel.filteredDressItems.observe(viewLifecycleOwner) { items ->
            if (items == null) return@observe
            overviewAdapter?.submitList(items)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchAllDressItems()
    }

    override fun onDetach() {
        host = null
        super.onDetach()
    }
}