package com.shadowcontacts.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shadowcontacts.app.BuildConfig
import com.shadowcontacts.app.R
import com.shadowcontacts.app.databinding.ActivityMainBinding
import com.shadowcontacts.app.utils.CallerIdPreferences
import com.shadowcontacts.app.utils.IOSStyleDialog
import com.shadowcontacts.app.utils.ImportExportHelper
import com.shadowcontacts.app.utils.ThemeHelper
import com.shadowcontacts.app.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ContactViewModel
    private lateinit var adapter: ContactAdapter

    private lateinit var etSearch: EditText
    private lateinit var ivClearSearch: ImageView

    // Import launcher
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importFromUri(it) } }

    // Export launcher
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { exportToUri(it) } }

    // Caller ID permissions launcher
    private val callerIdPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val phoneGranted = results[Manifest.permission.READ_PHONE_STATE] == true
        val callLogGranted = results[Manifest.permission.READ_CALL_LOG] == true
        if (phoneGranted && callLogGranted) {
            checkOverlayAndEnableCallerId()
        } else {
            Toast.makeText(this, "Phone & Call Log permissions are required for Caller ID", Toast.LENGTH_LONG).show()
        }
    }

    // Overlay settings return
    private val overlaySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            CallerIdPreferences.setEnabled(this, true)
            Toast.makeText(this, "Caller ID enabled", Toast.LENGTH_SHORT).show()
            invalidateOptionsMenu()
        } else {
            Toast.makeText(this, "Overlay permission required for Caller ID", Toast.LENGTH_LONG).show()
        }
    }

    // Contacts permission launcher for phone import
    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            performPhoneImport()
        } else {
            Toast.makeText(this, "Contacts permission required to import", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[ContactViewModel::class.java]

        setupRecyclerView()
        setupSearch()
        setupFab()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        if (adapter.isMultiSelectMode) exitMultiSelectMode()
    }

    // ── RecyclerView ──

    private fun setupRecyclerView() {
        adapter = ContactAdapter(
            onContactClick = { contact ->
                val intent = Intent(this, ContactDetailActivity::class.java)
                intent.putExtra("contact_id", contact.id)
                startActivity(intent)
            },
            onContactLongClick = { contact ->
                enterMultiSelectMode(contact)
                true
            },
            onCallClick = { contact ->
                if (contact.phone.isNotBlank()) {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
                }
            },
            onMessageClick = { contact ->
                if (contact.phone.isNotBlank()) {
                    startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phone}")))
                }
            },
            onWhatsAppClick = { contact ->
                openWhatsApp(contact.phone)
            },
            onSelectionChanged = { count ->
                if (adapter.isMultiSelectMode) {
                    binding.toolbarMultiSelect.title = "$count selected"
                }
            },
            onMultiSelectExited = {
                exitMultiSelectMode()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    // ── Search ──

    private fun setupSearch() {
        etSearch = findViewById(R.id.etSearch)
        ivClearSearch = findViewById(R.id.ivClearSearch)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()
                viewModel.setSearchQuery(query?.takeIf { it.isNotBlank() })
                ivClearSearch.visibility = if (query.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        ivClearSearch.setOnClickListener {
            etSearch.text.clear()
            etSearch.clearFocus()
        }
    }

    // ── FAB ──

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, ContactDetailActivity::class.java)
            intent.putExtra("group_id", viewModel.currentGroupId.value ?: 1L)
            startActivity(intent)
        }
    }

    // ── Observe ──

    private fun observeData() {
        viewModel.contacts.observe(this) { contacts ->
            adapter.submitData(contacts ?: emptyList())
            binding.emptyLayout.visibility = if (contacts.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }

    // ── Multi-select (Dailygraph-style) ──

    private fun enterMultiSelectMode(contact: com.shadowcontacts.app.data.Contact) {
        adapter.enterMultiSelectMode(contact)
        binding.toolbar.visibility = View.GONE
        binding.toolbarMultiSelect.visibility = View.VISIBLE
        binding.fabAdd.visibility = View.GONE

        binding.toolbarMultiSelect.menu.clear()
        binding.toolbarMultiSelect.setNavigationOnClickListener { exitMultiSelectMode() }
        binding.toolbarMultiSelect.inflateMenu(R.menu.menu_multi_select)
        binding.toolbarMultiSelect.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_selected -> {
                    deleteSelectedContacts()
                    true
                }
                R.id.action_edit_selected -> {
                    editSelectedContact()
                    true
                }
                R.id.action_select_all -> {
                    adapter.selectAll()
                    true
                }
                else -> false
            }
        }
    }

    private fun exitMultiSelectMode() {
        adapter.exitMultiSelectMode()
        binding.toolbarMultiSelect.visibility = View.GONE
        binding.toolbar.visibility = View.VISIBLE
        binding.fabAdd.visibility = View.VISIBLE
        binding.toolbarMultiSelect.menu.clear()
        binding.toolbarMultiSelect.setOnMenuItemClickListener(null)
    }

    private fun deleteSelectedContacts() {
        val selected = adapter.getSelectedContacts()
        IOSStyleDialog.showConfirm(
            this,
            "Delete ${selected.size} contact${if (selected.size != 1) "s" else ""}?",
            "This will permanently delete the selected contact${if (selected.size != 1) "s" else ""}.",
            isDanger = true
        ) {
            lifecycleScope.launch {
                val ids = selected.map { it.id }
                viewModel.deleteContactsByIdsDirect(ids)
                runOnUiThread {
                    exitMultiSelectMode()
                    Toast.makeText(this@MainActivity, "${selected.size} deleted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun editSelectedContact() {
        val selected = adapter.getSelectedContacts()
        if (selected.size != 1) {
            Toast.makeText(this, "Select exactly one contact to edit", Toast.LENGTH_SHORT).show()
            return
        }
        exitMultiSelectMode()
        val intent = Intent(this, ContactDetailActivity::class.java)
        intent.putExtra("contact_id", selected.first().id)
        intent.putExtra("start_edit", true)
        startActivity(intent)
    }

    // ── WhatsApp ──

    private fun openWhatsApp(phone: String) {
        if (phone.isBlank()) return
        val digits = phone.replace(Regex("[^0-9+]"), "")
        // If it doesn't start with +, assume Indian number
        val fullNumber = if (digits.startsWith("+")) digits else "+91$digits"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${fullNumber.removePrefix("+")}"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Menu ──

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val callerIdItem = menu.findItem(R.id.action_caller_id)
        callerIdItem?.isVisible = true
        if (BuildConfig.HAS_CALLER_ID) {
            callerIdItem?.isCheckable = true
            callerIdItem?.isChecked = CallerIdPreferences.isEnabled(this)
        } else {
            callerIdItem?.isCheckable = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import_phone -> {
                importFromPhone()
                true
            }
            R.id.action_import -> {
                importLauncher.launch(arrayOf("application/json", "*/*"))
                true
            }
            R.id.action_export -> {
                exportLauncher.launch("shadow_contacts_backup.json")
                true
            }
            R.id.action_appearance -> {
                IOSStyleDialog.showThemePicker(this, ThemeHelper.getSavedTheme(this)) { mode ->
                    ThemeHelper.saveTheme(this, mode)
                }
                true
            }
            R.id.action_caller_id -> {
                if (BuildConfig.HAS_CALLER_ID) {
                    toggleCallerId()
                } else {
                    showCallerIdPromo()
                }
                true
            }
            R.id.action_instructions -> {
                startActivity(Intent(this, InstructionsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ── Import / Export ──

    private fun importFromUri(uri: Uri) {
        lifecycleScope.launch {
            val result = ImportExportHelper.import(this@MainActivity, uri)
            result.onSuccess { r ->
                Toast.makeText(
                    this@MainActivity,
                    "Imported ${r.contactsImported} contacts" +
                        if (r.duplicatesSkipped > 0) " (${r.duplicatesSkipped} duplicates skipped)" else "",
                    Toast.LENGTH_LONG
                ).show()
            }
            result.onFailure { e ->
                Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportToUri(uri: Uri) {
        lifecycleScope.launch {
            val result = ImportExportHelper.export(this@MainActivity, uri)
            result.onSuccess { count ->
                Toast.makeText(this@MainActivity, "Exported $count contacts", Toast.LENGTH_SHORT).show()
            }
            result.onFailure { e ->
                Toast.makeText(this@MainActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Import from Phone ──

    private fun importFromPhone() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            return
        }
        performPhoneImport()
    }

    private fun performPhoneImport() {
        val intent = Intent(this, PhoneContactPickerActivity::class.java)
        intent.putExtra("group_id", viewModel.currentGroupId.value ?: 1L)
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (adapter.isMultiSelectMode) {
            exitMultiSelectMode()
        } else {
            super.onBackPressed()
        }
    }

    // ── Caller ID ──

    private fun showCallerIdPromo() {
        val builder = android.app.AlertDialog.Builder(this, R.style.IOSDialogTheme)
        builder.setTitle("Caller ID Popup")
        builder.setMessage(
            "Caller ID shows a popup with the contact's name and description when you receive a call.\n\n" +
            "Due to Google Play policy, this feature requires permissions that are not available in the Play Store version.\n\n" +
            "The full version with Caller ID is available as a free, open-source download on GitHub."
        )
        builder.setPositiveButton("Open GitHub") { dialog, _ ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    com.shadowcontacts.app.ui.InstructionsActivity.GITHUB_URL
                )))
            } catch (_: Exception) {}
            dialog.dismiss()
        }
        builder.setNegativeButton("Close", null)

        val alertDialog = builder.create()
        alertDialog.show()
        IOSStyleDialog.enforceMinWidthPublic(alertDialog)

        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            getColor(R.color.accent)
        )
        alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            getColor(R.color.accent)
        )
    }

    private fun toggleCallerId() {
        if (CallerIdPreferences.isEnabled(this)) {
            CallerIdPreferences.setEnabled(this, false)
            Toast.makeText(this, "Caller ID disabled", Toast.LENGTH_SHORT).show()
            invalidateOptionsMenu()
        } else {
            enableCallerId()
        }
    }

    private fun enableCallerId() {
        val needPhone = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        val needCallLog = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED

        if (needPhone || needCallLog) {
            callerIdPermissionsLauncher.launch(
                arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)
            )
            return
        }
        checkOverlayAndEnableCallerId()
    }

    private fun checkOverlayAndEnableCallerId() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow \"Display over other apps\"", Toast.LENGTH_LONG).show()
            overlaySettingsLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        CallerIdPreferences.setEnabled(this, true)
        Toast.makeText(this, "Caller ID enabled", Toast.LENGTH_SHORT).show()
        invalidateOptionsMenu()
    }
}
