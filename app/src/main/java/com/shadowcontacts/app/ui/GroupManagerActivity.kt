package com.shadowcontacts.app.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shadowcontacts.app.databinding.ActivityGroupManagerBinding
import com.shadowcontacts.app.utils.IOSStyleDialog
import com.shadowcontacts.app.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

class GroupManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupManagerBinding
    private lateinit var viewModel: ContactViewModel
    private lateinit var adapter: GroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Groups"

        viewModel = ViewModelProvider(this)[ContactViewModel::class.java]

        adapter = GroupAdapter(
            onRename = { group ->
                IOSStyleDialog.showInput(
                    this, "Rename Group", "Group name", group.name
                ) { newName ->
                    viewModel.renameGroup(group, newName)
                    Toast.makeText(this, "Renamed to $newName", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = { group ->
                lifecycleScope.launch {
                    val count = viewModel.getGroupCount()
                    if (count <= 1) {
                        Toast.makeText(this@GroupManagerActivity, "Cannot delete the last group", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val contactCount = viewModel.getContactCountForGroup(group.id)
                    IOSStyleDialog.showConfirm(
                        this@GroupManagerActivity,
                        "Delete Group",
                        "Delete \"${group.name}\" and its $contactCount contact${if (contactCount != 1) "s" else ""}?",
                        isDanger = true
                    ) {
                        viewModel.deleteGroup(group) {
                            runOnUiThread {
                                Toast.makeText(this@GroupManagerActivity, "Group deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabAddGroup.setOnClickListener {
            IOSStyleDialog.showInput(this, "New Group", "Group name") { name ->
                viewModel.insertGroup(name)
                Toast.makeText(this, "Group created", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.allGroups.observe(this) { groups ->
            if (groups != null) {
                lifecycleScope.launch {
                    val counts = mutableMapOf<Long, Int>()
                    for (g in groups) {
                        counts[g.id] = viewModel.getContactCountForGroup(g.id)
                    }
                    adapter.submitData(groups, counts)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
