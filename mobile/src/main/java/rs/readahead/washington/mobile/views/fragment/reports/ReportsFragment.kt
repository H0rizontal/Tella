package rs.readahead.washington.mobile.views.fragment.reports

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayoutMediator
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.DRAFT_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.OUTBOX_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.ViewPagerAdapter


class ReportsFragment :
    BaseBindingFragment<FragmentReportsBinding>(FragmentReportsBinding::inflate) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        val viewPagerAdapter = ViewPagerAdapter(this)

        binding?.viewPager?.apply {
            offscreenPageLimit = 3
            isSaveEnabled = false
            adapter = viewPagerAdapter
        }
        // Set the text for each tab
        binding?.viewPager?.let {
            TabLayoutMediator(binding?.tabs!!, it) { tab, position ->
                tab.text = getTabTitle(position)

            }.attach()
        }

        binding?.tabs?.setTabTextColors(
            ContextCompat.getColor(baseActivity, R.color.wa_white_44),
            ContextCompat.getColor(baseActivity, R.color.wa_white)
        )

        binding?.newReportBtn?.setOnClickListener {
            nav().navigate(R.id.action_reportsScreen_to_newReport_screen)
        }

    }

    private fun getTabTitle(position: Int): String? {
        return when (position) {
            DRAFT_LIST_PAGE_INDEX -> getString(R.string.collect_draft_tab_title)
            OUTBOX_LIST_PAGE_INDEX -> getString(R.string.collect_outbox_tab_title)
            SUBMITTED_LIST_PAGE_INDEX -> getString(R.string.collect_sent_tab_title)
            else -> null
        }
    }

    private fun initData() {

    }

}