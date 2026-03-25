package com.shadowcontacts.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shadowcontacts.app.R
import com.shadowcontacts.app.data.Contact
import com.shadowcontacts.app.data.ContactDatabase
import com.shadowcontacts.app.databinding.ActivityPhoneContactPickerBinding
import com.shadowcontacts.app.utils.PhoneContactImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhoneContactPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneContactPickerBinding
    private lateinit var adapter: PhoneContactPickAdapter

    private var allPhoneContacts: List<PhoneContactImporter.PhoneContactEntry> = emptyList()
    private var filteredContacts: List<PhoneContactImporter.PhoneContactEntry> = emptyList()
    private var duplicateNames: Set<String> = emptySet()
    private val selectedIndices = mutableSetOf<Int>()

    private var groupId: Long = 1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneContactPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        groupId = intent.getLongExtra("group_id", 1L)

        adapter = PhoneContactPickAdapter(
            onToggle = { position ->
                val realIndex = getRealIndex(position)
                if (selectedIndices.contains(realIndex)) {
                    selectedIndices.remove(realIndex)
                } else {
                    selectedIndices.add(realIndex)
                }
                adapter.notifyItemChanged(position)
                updateSelectionCount()
            },
            isSelected = { position ->
                selectedIndices.contains(getRealIndex(position))
            },
            isDuplicate = { entry ->
                duplicateNames.contains(normalizeForDupe(entry.name, entry.phone))
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        setupSearch()
        setupButtons()
        loadContacts()
    }

    private fun getRealIndex(filteredPosition: Int): Int {
        val entry = filteredContacts[filteredPosition]
        return allPhoneContacts.indexOf(entry)
    }

    private fun normalizeForDupe(name: String, phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        val normPhone = if (digits.length >= 10) digits.takeLast(10) else digits
        return "${name.lowercase().trim()}|$normPhone"
    }

    private fun setupSearch() {
        val etSearch: EditText = findViewById(R.id.etSearchPicker)
        val ivClear: ImageView = findViewById(R.id.ivClearPickerSearch)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                ivClear.visibility = if (query.isBlank()) View.GONE else View.VISIBLE
                filterContacts(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        ivClear.setOnClickListener {
            etSearch.text.clear()
            etSearch.clearFocus()
        }
    }

    private fun filterContacts(query: String) {
        filteredContacts = if (query.isBlank()) {
            allPhoneContacts
        } else {
            allPhoneContacts.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.phone.contains(query)
            }
        }
        adapter.submitData(filteredContacts)
    }

    private fun setupButtons() {
        binding.btnSelectAll.setOnClickListener {
            selectedIndices.clear()
            for (i in allPhoneContacts.indices) {
                selectedIndices.add(i)
            }
            adapter.notifyDataSetChanged()
            updateSelectionCount()
        }

        binding.btnDeselectAll.setOnClickListener {
            selectedIndices.clear()
            adapter.notifyDataSetChanged()
            updateSelectionCount()
        }

        binding.btnImportSelected.setOnClickListener {
            importSelected()
        }
    }

    private fun updateSelectionCount() {
        val count = selectedIndices.size
        binding.tvSelectionCount.text = "$count selected"
        binding.btnImportSelected.text = if (count > 0) "Import $count Contact${if (count != 1) "s" else ""}" else "Import Selected"
        binding.btnImportSelected.isEnabled = count > 0
    }

    private fun loadContacts() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val contacts = withContext(Dispatchers.IO) {
                PhoneContactImporter.readPhoneContacts(this@PhoneContactPickerActivity)
            }

            // Find existing duplicates
            val dao = ContactDatabase.getDatabase(this@PhoneContactPickerActivity).contactDao()
            val existing = withContext(Dispatchers.IO) { dao.getAllContactsSync() }
            duplicateNames = existing.map { normalizeForDupe(it.name, it.phone) }.toSet()

            allPhoneContacts = contacts.sortedBy { it.name.lowercase() }
            filteredContacts = allPhoneContacts

            binding.progressBar.visibility = View.GONE
            adapter.submitData(filteredContacts)
            binding.tvSelectionCount.text = "${allPhoneContacts.size} contacts found"
        }
    }

    private fun importSelected() {
        if (selectedIndices.isEmpty()) return

        val toImport = selectedIndices.mapNotNull { allPhoneContacts.getOrNull(it) }

        lifecycleScope.launch {
            val dao = ContactDatabase.getDatabase(this@PhoneContactPickerActivity).contactDao()
            var imported = 0
            var skippedDupes = 0

            withContext(Dispatchers.IO) {
                for (entry in toImport) {
                    val dupeKey = normalizeForDupe(entry.name, entry.phone)
                    if (duplicateNames.contains(dupeKey)) {
                        skippedDupes++
                        continue
                    }
                    dao.insertContact(
                        Contact(
                            groupId = groupId,
                            prefix = entry.prefix,
                            name = entry.name,
                            suffix = entry.suffix,
                            phone = entry.phone,
                            description = ""
                        )
                    )
                    imported++
                }
            }

            val msg = buildString {
                append("Imported $imported contact${if (imported != 1) "s" else ""}")
                if (skippedDupes > 0) append(" ($skippedDupes duplicates skipped)")
            }
            Toast.makeText(this@PhoneContactPickerActivity, msg, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
