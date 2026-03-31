package com.shadowcontacts.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.shadowcontacts.app.R
import com.shadowcontacts.app.data.Contact

class ContactAdapter(
    private val onContactClick: (Contact) -> Unit,
    private val onContactLongClick: (Contact) -> Boolean,
    private val onCallClick: (Contact) -> Unit,
    private val onMessageClick: (Contact) -> Unit,
    private val onWhatsAppClick: (Contact) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = {},
    private val onMultiSelectExited: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTACT = 1
    }

    private var items: List<Any> = emptyList() // String (header) or Contact
    private val selectedIds = mutableSetOf<Long>()
    var isMultiSelectMode = false
        private set

    fun submitData(contacts: List<Contact>) {
        val newItems = mutableListOf<Any>()
        var lastLetter = ""

        for (contact in contacts) {
            val firstChar = contact.name.firstOrNull()?.uppercase() ?: "#"
            val letter = if (firstChar.matches(Regex("[A-Z]"))) firstChar else "#"
            if (letter != lastLetter) {
                newItems.add(letter)
                lastLetter = letter
            }
            newItems.add(contact)
        }

        items = newItems
        notifyDataSetChanged()
    }

    // ── Multi-select ──

    fun enterMultiSelectMode(contact: Contact) {
        isMultiSelectMode = true
        selectedIds.add(contact.id)
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun toggleSelection(contact: Contact) {
        if (selectedIds.contains(contact.id)) selectedIds.remove(contact.id)
        else selectedIds.add(contact.id)
        val pos = items.indexOfFirst { it is Contact && it.id == contact.id }
        if (pos >= 0) notifyItemChanged(pos)
        onSelectionChanged(selectedIds.size)
        if (selectedIds.isEmpty()) {
            isMultiSelectMode = false
            notifyDataSetChanged()
            onMultiSelectExited()
        }
    }

    fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(items.filterIsInstance<Contact>().map { it.id })
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun getSelectedContacts(): List<Contact> =
        items.filterIsInstance<Contact>().filter { selectedIds.contains(it.id) }

    fun getSelectedCount(): Int = selectedIds.size

    fun getAllContactIds(): List<Long> = items.filterIsInstance<Contact>().map { it.id }

    fun isEmpty(): Boolean = items.filterIsInstance<Contact>().isEmpty()

    // ── ViewHolder ──

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int =
        if (items[position] is String) TYPE_HEADER else TYPE_CONTACT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_section_header, parent, false))
        } else {
            ContactViewHolder(inflater.inflate(R.layout.item_contact, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position] as String)
            is ContactViewHolder -> holder.bind(items[position] as Contact)
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.sectionHeaderText)
        fun bind(letter: String) { text.text = letter }
    }

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card: CardView = view.findViewById(R.id.contactCard)
        private val checkbox: CheckBox = view.findViewById(R.id.checkboxSelect)
        private val nameText: TextView = view.findViewById(R.id.contactName)
        private val phoneText: TextView = view.findViewById(R.id.contactPhone)
        private val descText: TextView = view.findViewById(R.id.contactDescription)
        private val initialsText: TextView = view.findViewById(R.id.contactInitials)
        private val actionButtons: LinearLayout = view.findViewById(R.id.actionButtons)
        private val callBtn: ImageButton = view.findViewById(R.id.btnCall)
        private val msgBtn: ImageButton = view.findViewById(R.id.btnMessage)
        private val waBtn: ImageButton = view.findViewById(R.id.btnWhatsApp)

        fun bind(contact: Contact) {
            nameText.text = contact.displayName()

            // Phone
            if (contact.phone.isNotBlank()) {
                phoneText.text = contact.phone
                phoneText.visibility = View.VISIBLE
            } else {
                phoneText.visibility = View.GONE
            }

            // Description
            if (contact.description.isNotBlank()) {
                descText.text = contact.description
                descText.visibility = View.VISIBLE
            } else {
                descText.visibility = View.GONE
            }

            // Initials
            val initials = contact.name.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
            initialsText.text = initials.ifEmpty { "?" }

            // Multi-select mode
            if (isMultiSelectMode) {
                checkbox.visibility = View.VISIBLE
                checkbox.isChecked = selectedIds.contains(contact.id)
                actionButtons.visibility = View.GONE
            } else {
                checkbox.visibility = View.GONE
                // Show action buttons only if phone exists
                if (contact.phone.isNotBlank()) {
                    actionButtons.visibility = View.VISIBLE
                    callBtn.visibility = View.VISIBLE
                    msgBtn.visibility = View.VISIBLE
                    waBtn.visibility = View.VISIBLE
                    // Clear tint programmatically — some OEM ROMs apply a default tint
                    // to ImageButton via theme, overriding the green WhatsApp icon color.
                    waBtn.imageTintList = null
                } else {
                    actionButtons.visibility = View.GONE
                }
            }

            // Click handlers
            card.setOnClickListener {
                if (isMultiSelectMode) toggleSelection(contact)
                else onContactClick(contact)
            }
            card.setOnLongClickListener {
                if (!isMultiSelectMode) onContactLongClick(contact) else false
            }

            callBtn.setOnClickListener { onCallClick(contact) }
            msgBtn.setOnClickListener { onMessageClick(contact) }
            waBtn.setOnClickListener { onWhatsAppClick(contact) }
        }
    }
}
