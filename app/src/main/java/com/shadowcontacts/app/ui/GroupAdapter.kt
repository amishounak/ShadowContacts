package com.shadowcontacts.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shadowcontacts.app.R
import com.shadowcontacts.app.data.Group

class GroupAdapter(
    private val onRename: (Group) -> Unit,
    private val onDelete: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private var groups: List<Group> = emptyList()
    private var contactCounts: Map<Long, Int> = emptyMap()

    fun submitData(groups: List<Group>, counts: Map<Long, Int>) {
        this.groups = groups
        this.contactCounts = counts
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = groups.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.groupName)
        private val countText: TextView = view.findViewById(R.id.groupCount)
        private val renameBtn: ImageButton = view.findViewById(R.id.btnRename)
        private val deleteBtn: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(group: Group) {
            nameText.text = group.name
            val count = contactCounts[group.id] ?: 0
            countText.text = "$count contact${if (count != 1) "s" else ""}"

            renameBtn.setOnClickListener { onRename(group) }
            deleteBtn.setOnClickListener { onDelete(group) }
        }
    }
}
