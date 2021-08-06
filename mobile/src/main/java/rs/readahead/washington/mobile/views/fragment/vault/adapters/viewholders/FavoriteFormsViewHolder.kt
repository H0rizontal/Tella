package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.forms.FavoriteFormsAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class FavoriteFormsViewHolder  (val view : View) : BaseViewHolder<List<XFormEntity>>(view) {
    private lateinit var favoriteFormsRecyclerView : RecyclerView

    override fun bind(item: List<XFormEntity>, vararg args: Any) {
        favoriteFormsRecyclerView = view.findViewById(R.id.favoriteFormsRecyclerView)
        favoriteFormsRecyclerView.apply {
            adapter = FavoriteFormsAdapter(item, args[0] as VaultClickListener)
        }

    }

    companion object {
        fun from(parent: ViewGroup): FavoriteFormsViewHolder {
            return FavoriteFormsViewHolder(parent.inflate(R.layout.item_vault_favorite_forms))
        }
    }

}