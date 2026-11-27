package de.juvento.plain.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import de.juvento.plain.R
import de.juvento.plain.data.AppModel
import de.juvento.plain.data.AppSearch
import de.juvento.plain.databinding.AdapterAppDrawerBinding
import de.juvento.plain.helper.dp2px
import de.juvento.plain.style.UiStyle

class AppDrawerAdapter(
    private val gravity: Int,
    private val textSize: Int,
    /** List favorites first, followed by an empty row and all other apps. */
    private val groupFavorites: Boolean,
    private val style: UiStyle.Snapshot,
    private val clickListener: (AppModel) -> Unit,
    private val longClickListener: (AppModel) -> Unit,
    /** Called when a search leaves exactly one app; the caller decides whether to launch it. */
    private val singleMatchListener: (AppModel) -> Unit,
) : RecyclerView.Adapter<AppDrawerAdapter.ViewHolder>(), Filterable {

    private var appsList: MutableList<AppModel> = mutableListOf()
    private var filteredList: List<AppModel> = emptyList()

    // What is shown: favorites, a null entry for the empty separator row, then the other apps.
    private var rows: List<AppModel?> = emptyList()
    private var query = ""

    val apps: List<AppModel> get() = appsList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.appTitle.textSize = textSize.toFloat()
        binding.appTitle.gravity = gravity
        style.apply(binding.appTitle)
        return ViewHolder(binding.appTitle)
    }

    override fun getItemViewType(position: Int): Int = if (rows[position] == null) TYPE_SEPARATOR else TYPE_APP

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = rows[position]
        if (app == null) {
            holder.bindSeparator()
            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener(null)
            holder.itemView.isClickable = false
            holder.itemView.isLongClickable = false
            return
        }
        holder.bind(app, gravity, textSize)
        holder.itemView.setOnClickListener { clickListener(app) }
        holder.itemView.setOnLongClickListener {
            longClickListener(app)
            true
        }
    }

    override fun getItemCount(): Int = rows.size

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val text = constraint?.toString().orEmpty().trim()
            val result = if (text.isEmpty()) appsList.toList() else appsList.filter { AppSearch.matches(it, text) }
            return FilterResults().apply { values = result }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            query = constraint?.toString().orEmpty().trim()
            filteredList = results.values as List<AppModel>
            rows = buildRows(filteredList)
            notifyDataSetChanged()
            if (query.isNotEmpty() && filteredList.size == 1) singleMatchListener(filteredList[0])
        }
    }

    private fun buildRows(apps: List<AppModel>): List<AppModel?> {
        if (!groupFavorites) return apps
        val (favorites, others) = apps.partition { it.isFavorite }
        return if (favorites.isNotEmpty() && others.isNotEmpty()) favorites + listOf(null) + others else favorites + others
    }

    fun setAppList(apps: MutableList<AppModel>) {
        appsList = apps
        filter.filter(query)
    }

    /** Re-sorts and re-filters after an app has been renamed or its keywords changed. */
    fun refresh() {
        appsList.sortBy { it.name.lowercase() }
        filter.filter(query)
    }

    fun remove(app: AppModel) {
        appsList.remove(app)
        filter.filter(query)
    }

    fun launchFirstInList() {
        rows.firstOrNull { it != null }?.let(clickListener)
    }

    private companion object {
        const val TYPE_APP = 0
        const val TYPE_SEPARATOR = 1
    }

    class ViewHolder(private val title: TextView) : RecyclerView.ViewHolder(title) {
        fun bindSeparator() {
            title.text = "\u00A0"
            title.setCompoundDrawables(null, null, null, null)
        }

        fun bind(app: AppModel, gravity: Int, textSize: Int) {
            title.text = app.name

            // icon next to the name to show that this app lives in another (work) profile
            if (app.user != android.os.Process.myUserHandle()) {
                val icon = AppCompatResources.getDrawable(title.context, R.drawable.work_profile)
                val px = dp2px(title.resources, textSize)
                icon?.setBounds(0, 0, px, px)
                icon?.setTint(title.currentTextColor)
                val isLeft = gravity == android.view.Gravity.LEFT
                title.setCompoundDrawables(if (isLeft) null else icon, null, if (isLeft) icon else null, null)
                title.compoundDrawablePadding = 20
            } else {
                title.setCompoundDrawables(null, null, null, null)
            }
        }
    }
}
