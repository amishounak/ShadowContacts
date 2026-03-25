package com.shadowcontacts.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shadowcontacts.app.R
import com.shadowcontacts.app.utils.PhoneContactImporter

class PhoneContactPickAdapter(
    private val onToggle: (Int) -> Unit,
    private val isSelected: (Int) -> Boolean,
    private val isDuplicate: (PhoneContactImporter.PhoneContactEntry) -> Boolean
) : RecyclerView.Adapter<PhoneContactPickAdapter.ViewHolder>() {

    private var contacts: List<PhoneContactImporter.PhoneContactEntry> = emptyList()

    fun submitData(data: List<PhoneContactImporter.PhoneContactEntry>) {
        contacts = data
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = contacts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_phone_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(contacts[position], position)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val checkbox: CheckBox = view.findViewById(R.id.checkboxPick)
        private val initials: TextView = view.findViewById(R.id.pickInitials)
        private val name: TextView = view.findViewById(R.id.pickName)
        private val phone: TextView = view.findViewById(R.id.pickPhone)
        private val dupeBadge: TextView = view.findViewById(R.id.pickDuplicateBadge)

        fun bind(entry: PhoneContactImporter.PhoneContactEntry, position: Int) {
            val displayName = buildString {
                if (entry.prefix.isNotBlank()) append("${entry.prefix} ")
                append(entry.name)
                if (entry.suffix.isNotBlank()) append(" ${entry.suffix}")
            }.trim()

            name.text = displayName
            phone.text = entry.phone.ifBlank { "No phone" }
            checkbox.isChecked = isSelected(position)

            // Initials
            val ini = entry.name.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
            initials.text = ini.ifEmpty { "?" }

            // Duplicate badge
            if (isDuplicate(entry)) {
                dupeBadge.visibility = View.VISIBLE
            } else {
                dupeBadge.visibility = View.GONE
            }

            itemView.setOnClickListener { onToggle(position) }
        }
    }
}
