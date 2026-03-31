package com.shadowcontacts.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.shadowcontacts.app.R
import com.shadowcontacts.app.data.Contact
import com.shadowcontacts.app.databinding.ActivityContactDetailBinding
import com.shadowcontacts.app.utils.IOSStyleDialog
import com.shadowcontacts.app.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

class ContactDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactDetailBinding
    private lateinit var viewModel: ContactViewModel

    private var contactId: Long = -1L
    private var groupId: Long = 1L
    private var existingContact: Contact? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[ContactViewModel::class.java]

        contactId = intent.getLongExtra("contact_id", -1L)
        groupId = intent.getLongExtra("group_id", 1L)

        if (contactId > 0) {
            // Editing existing contact — start in view mode
            loadContact()
        } else {
            // Creating new contact — start in edit mode
            supportActionBar?.title = "New Contact"
            isEditMode = true
            setFieldsEditable(true)
        }

        setupActionButtons()
    }

    private fun loadContact() {
        lifecycleScope.launch {
            val contact = viewModel.getContactById(contactId)
            if (contact == null) {
                Toast.makeText(this@ContactDetailActivity, "Contact not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            existingContact = contact
            groupId = contact.groupId

            binding.editPrefix.setText(contact.prefix)
            binding.editName.setText(contact.name)
            binding.editSuffix.setText(contact.suffix)
            binding.editPhone.setText(contact.phone)
            binding.editDescription.setText(contact.description)

            supportActionBar?.title = contact.displayName()

            // Check if we should auto-enter edit mode (from multi-select Edit)
            if (intent.getBooleanExtra("start_edit", false)) {
                isEditMode = true
                setFieldsEditable(true)
                supportActionBar?.title = "Edit Contact"
            } else {
                isEditMode = false
                setFieldsEditable(false)
            }
            invalidateOptionsMenu()
        }
    }

    private fun setFieldsEditable(editable: Boolean) {
        val fields = listOf(
            binding.editPrefix, binding.editName, binding.editSuffix,
            binding.editPhone, binding.editDescription
        )
        for (field in fields) {
            field.isFocusable = editable
            field.isFocusableInTouchMode = editable
            field.isCursorVisible = editable
        }
        if (editable) {
            binding.editName.requestFocus()
        }
    }

    private fun setupActionButtons() {
        // Force-clear the icon tint on WhatsApp button programmatically.
        // app:iconTint="@null" in XML is ignored by MaterialButton on some devices/OEM ROMs,
        // causing the green WhatsApp icon to be tinted grey/blue. Setting it in code is reliable.
        binding.btnWhatsApp.iconTint = null

        binding.btnCall.setOnClickListener {
            val phone = binding.editPhone.text.toString().trim()
            if (phone.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            } else {
                Toast.makeText(this, "No phone number", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMessage.setOnClickListener {
            val phone = binding.editPhone.text.toString().trim()
            if (phone.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")))
            } else {
                Toast.makeText(this, "No phone number", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnWhatsApp.setOnClickListener {
            val phone = binding.editPhone.text.toString().trim()
            if (phone.isNotBlank()) {
                val digits = phone.replace(Regex("[^0-9+]"), "")
                val fullNumber = if (digits.startsWith("+")) digits else "+91$digits"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${fullNumber.removePrefix("+")}")))
                } catch (e: Exception) {
                    Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Menu ──

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_contact_detail, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (existingContact == null) {
            // New contact mode: only Save visible
            menu.findItem(R.id.action_save)?.isVisible = true
            menu.findItem(R.id.action_edit)?.isVisible = false
            menu.findItem(R.id.action_delete)?.isVisible = false
            menu.findItem(R.id.action_cancel_edit)?.isVisible = false
        } else if (isEditMode) {
            // Editing existing
            menu.findItem(R.id.action_save)?.isVisible = true
            menu.findItem(R.id.action_edit)?.isVisible = false
            menu.findItem(R.id.action_delete)?.isVisible = false
            menu.findItem(R.id.action_cancel_edit)?.isVisible = true
        } else {
            // View mode
            menu.findItem(R.id.action_save)?.isVisible = false
            menu.findItem(R.id.action_edit)?.isVisible = true
            menu.findItem(R.id.action_delete)?.isVisible = true
            menu.findItem(R.id.action_cancel_edit)?.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveContact()
                true
            }
            R.id.action_edit -> {
                isEditMode = true
                setFieldsEditable(true)
                supportActionBar?.title = "Edit Contact"
                invalidateOptionsMenu()
                true
            }
            R.id.action_cancel_edit -> {
                // Restore original values
                existingContact?.let { c ->
                    binding.editPrefix.setText(c.prefix)
                    binding.editName.setText(c.name)
                    binding.editSuffix.setText(c.suffix)
                    binding.editPhone.setText(c.phone)
                    binding.editDescription.setText(c.description)
                    supportActionBar?.title = c.displayName()
                }
                isEditMode = false
                setFieldsEditable(false)
                invalidateOptionsMenu()
                true
            }
            R.id.action_delete -> {
                IOSStyleDialog.showConfirm(
                    this,
                    "Delete Contact",
                    "Delete ${existingContact?.displayName()}?",
                    isDanger = true
                ) {
                    existingContact?.let { viewModel.deleteContact(it) }
                    Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveContact() {
        val name = binding.editName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }

        val prefix = binding.editPrefix.text.toString().trim()
        val suffix = binding.editSuffix.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val description = binding.editDescription.text.toString().trim()

        if (existingContact != null) {
            // Update existing
            val updated = existingContact!!.copy(
                prefix = prefix,
                name = name,
                suffix = suffix,
                phone = phone,
                description = description,
                updatedAt = System.currentTimeMillis()
            )
            viewModel.updateContact(updated)
            Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show()
        } else {
            // Create new
            val newContact = Contact(
                groupId = groupId,
                prefix = prefix,
                name = name,
                suffix = suffix,
                phone = phone,
                description = description
            )
            viewModel.insertContact(newContact)
            Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
