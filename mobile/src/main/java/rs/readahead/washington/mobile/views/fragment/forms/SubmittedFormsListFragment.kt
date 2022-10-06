package rs.readahead.washington.mobile.views.fragment.forms

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionSeleceted
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent
import rs.readahead.washington.mobile.databinding.FragmentSubmittedFormsListBinding
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance
import rs.readahead.washington.mobile.views.adapters.CollectSubmittedFormInstanceRecycleViewAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface
import timber.log.Timber

class SubmittedFormsListFragment : BaseBindingFragment<FragmentSubmittedFormsListBinding>(
    FragmentSubmittedFormsListBinding::inflate
),
    FormListInterfce, ISavedFormsInterface {
    private val model: SharedFormsViewModel by lazy {
        ViewModelProvider(baseActivity).get(SharedFormsViewModel::class.java)
    }

    private var adapter: CollectSubmittedFormInstanceRecycleViewAdapter? = null
    override fun getFormListType(): FormListInterfce.Type {
        return FormListInterfce.Type.SUBMITTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = CollectSubmittedFormInstanceRecycleViewAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
        listSubmittedForms()
    }

    private fun initObservers() {
        model.onFormInstanceDeleteSuccess.observe(
            viewLifecycleOwner,
            { success: Boolean? ->
                onFormInstanceDeleted(
                    success!!
                )
            })

        model.onSubmittedFormInstanceListSuccess.observe(
            viewLifecycleOwner
        ) { instances: List<CollectFormInstance> ->
            onFormInstanceListSuccess(
                instances
            )
        }

        model.onFormInstanceListError.observe(
            viewLifecycleOwner
        ) { error: Throwable? ->
            onFormInstanceListError(
                error
            )
        }
    }

    private fun onFormInstanceDeleted(success: Boolean) {
        if (success) {
            DialogUtils.showBottomMessage(
                activity,
                getString(R.string.collect_toast_form_deleted),
                false
            )
            listSubmittedForms()
        }
    }

    private fun onFormInstanceListSuccess(instances: List<CollectFormInstance?>) {
        binding?.blankSubmittedFormsInfo?.visibility = if (instances.isEmpty()) View.VISIBLE else View.GONE
        adapter!!.setInstances(instances)
    }

    private fun onFormInstanceListError(error: Throwable?) {
        Timber.d(error, javaClass.name)
    }

    fun listSubmittedForms() {
        model.listSubmitFormInstances()
    }

    override fun showFormsMenu(instance: CollectFormInstance) {
        BottomSheetUtils.showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            instance.instanceName,
            requireContext().getString(R.string.collect_sent_action_edit_to_resend),
            requireContext().getString(R.string.action_delete),
            object : ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        //MyApplication.bus().post(new ReSubmitFormInstanceEvent(instance));
                        MyApplication.bus().post(ShowFormInstanceEntryEvent(instance.id))
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        deleteFormInstance(instance.id)
                    }
                }
            },
            requireContext().getString(R.string.Collect_DeleteForm_SheetTitle),
            requireContext().getString(R.string.collect_dialog_text_delete_sent_form),
            requireContext().getString(R.string.action_delete),
            requireContext().getString(R.string.action_cancel)
        )
    }

    override fun reSubmitForm(instance: CollectFormInstance?) {}
    fun deleteFormInstance(instanceId: Long) {
        model.deleteFormInstance(instanceId)
    }

    companion object {
        fun newInstance(): SubmittedFormsListFragment {
            return SubmittedFormsListFragment()
        }
    }

    fun initView(){
        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        binding?.submittFormInstances?.layoutManager = mLayoutManager
        binding?.submittFormInstances?.adapter = adapter
    }
}