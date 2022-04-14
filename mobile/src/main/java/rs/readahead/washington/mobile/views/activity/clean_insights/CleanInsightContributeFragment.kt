package rs.readahead.washington.mobile.views.activity.clean_insights

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hzontal.utils.Util
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class CleanInsightContributeFragment : BaseFragment() {

    private lateinit var onNext: (CleanInsightsActions) -> Unit
    private lateinit var onPrevious: () -> Unit

    companion object {
        private const val URL_MORE_INFO = "https://docs.tella-app.org/v/faq/privacy/analytics"

        @JvmStatic
        fun newInstance(
            onNext: (CleanInsightsActions) -> Unit,
            onPrevious: () -> Unit
        ) =
            CleanInsightContributeFragment().apply {
                this.onNext = onNext
                this.onPrevious = onPrevious
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_clean_insights_contribute, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        view.findViewById<TextView>(R.id.back_btn).setOnClickListener { onPrevious() }
        view.findViewById<TextView>(R.id.btn_yes).setOnClickListener { onNext(CleanInsightsActions.YES) }
        view.findViewById<TextView>(R.id.btn_no).setOnClickListener { onNext(CleanInsightsActions.NO) }
        setMoreInfoText(view.findViewById(R.id.tv_more_info))
    }

    private fun setMoreInfoText(textView: TextView) {
        with(textView) {
            text =""
            val firstPart = SpannableString(getString(R.string.clean_insights_contribute_more_info1))
            val link = SpannableString(getString(R.string.clean_insights_contribute_more_info2))
            link.setSpan(getClickableSpan(), 0, link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            movementMethod = LinkMovementMethod.getInstance()
            append(firstPart)
            append(link)
        }
    }

    private fun getClickableSpan() = object : ClickableSpan() {
        override fun onClick(textView: View) {
            Util.startBrowserIntent(requireContext(), URL_MORE_INFO)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.color = ContextCompat.getColor(requireContext(), R.color.wa_orange)
            ds.isUnderlineText = false
        }
    }

}