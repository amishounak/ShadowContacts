package com.shadowcontacts.app.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.shadowcontacts.app.data.Contact
import com.shadowcontacts.app.data.ContactDatabase
import com.shadowcontacts.app.data.Group
import com.shadowcontacts.app.repository.ContactRepository
import com.shadowcontacts.app.utils.GroupPreferences
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ContactRepository = ContactRepository(
        ContactDatabase.getDatabase(application).contactDao()
    )
    val allGroups: LiveData<List<Group>> = repository.getAllGroups()

    // Current group
    private val _currentGroupId = MutableLiveData<Long>().apply {
        value = GroupPreferences.getActiveGroupId(application)
    }
    val currentGroupId: LiveData<Long> get() = _currentGroupId

    // Search query
    private val _searchQuery = MutableLiveData<String?>()

    // Contacts list — switches between filtered and unfiltered
    val contacts: LiveData<List<Contact>> = MediatorLiveData<List<Contact>>().apply {
        var currentSource: LiveData<List<Contact>>? = null

        fun refresh() {
            val groupId = _currentGroupId.value ?: return
            val query = _searchQuery.value

            currentSource?.let { removeSource(it) }

            val newSource = if (query.isNullOrBlank()) {
                repository.getContactsByGroup(groupId)
            } else {
                repository.searchContacts(groupId, query)
            }

            currentSource = newSource
            addSource(newSource) { value = it }
        }

        addSource(_currentGroupId) { refresh() }
        addSource(_searchQuery) { refresh() }
    }

    // Multi-select state
    private val _selectedIds = MutableLiveData<Set<Long>>(emptySet())
    val selectedIds: LiveData<Set<Long>> get() = _selectedIds

    private val _isMultiSelectMode = MutableLiveData(false)
    val isMultiSelectMode: LiveData<Boolean> get() = _isMultiSelectMode

    // ── Group switching ──

    fun switchGroup(groupId: Long) {
        _currentGroupId.value = groupId
        GroupPreferences.setActiveGroupId(getApplication(), groupId)
        clearMultiSelect()
    }

    // ── Search ──

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
    }

    // ── Contact CRUD ──

    fun insertContact(contact: Contact, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.insertContact(contact)
            onComplete?.invoke(id)
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            repository.updateContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun deleteSelectedContacts(onComplete: (() -> Unit)? = null) {
        val ids = _selectedIds.value?.toList() ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteContactsByIds(ids)
            clearMultiSelect()
            onComplete?.invoke()
        }
    }

    suspend fun getContactById(id: Long): Contact? = repository.getContactById(id)

    suspend fun deleteContactsByIdsDirect(ids: List<Long>) = repository.deleteContactsByIds(ids)

    // ── Multi-select ──

    fun toggleSelection(contactId: Long) {
        val current = _selectedIds.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(contactId)) current.remove(contactId) else current.add(contactId)
        _selectedIds.value = current
        if (current.isEmpty()) _isMultiSelectMode.value = false
    }

    fun enterMultiSelect(contactId: Long) {
        _isMultiSelectMode.value = true
        _selectedIds.value = setOf(contactId)
    }

    fun selectAll(allIds: List<Long>) {
        _selectedIds.value = allIds.toSet()
    }

    fun clearMultiSelect() {
        _selectedIds.value = emptySet()
        _isMultiSelectMode.value = false
    }

    // ── Group CRUD ──

    fun insertGroup(name: String, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.insertGroup(Group(name = name))
            onComplete?.invoke(id)
        }
    }

    fun renameGroup(group: Group, newName: String) {
        viewModelScope.launch {
            repository.updateGroup(group.copy(name = newName))
        }
    }

    fun deleteGroup(group: Group, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteContactsForGroup(group.id)
            repository.deleteGroup(group)
            // If deleted group was active, switch to first available
            if (_currentGroupId.value == group.id) {
                val groups = repository.getAllGroupsSync()
                if (groups.isNotEmpty()) {
                    switchGroup(groups.first().id)
                }
            }
            onComplete?.invoke()
        }
    }

    suspend fun getGroupCount(): Int = repository.getGroupCount()

    // ── Import/Export helpers ──

    suspend fun getAllGroupsSync(): List<Group> = repository.getAllGroupsSync()
    suspend fun getAllContactsSync(): List<Contact> = repository.getAllContactsSync()
    suspend fun getContactCountForGroup(groupId: Long): Int = repository.getContactCountForGroup(groupId)
}
