package com.shadowcontacts.app.repository

import androidx.lifecycle.LiveData
import com.shadowcontacts.app.data.Contact
import com.shadowcontacts.app.data.ContactDao
import com.shadowcontacts.app.data.Group

class ContactRepository(private val dao: ContactDao) {

    // ── Contacts ──

    fun getContactsByGroup(groupId: Long): LiveData<List<Contact>> =
        dao.getContactsByGroup(groupId)

    fun searchContacts(groupId: Long, query: String): LiveData<List<Contact>> =
        dao.searchContacts(groupId, query)

    suspend fun insertContact(contact: Contact): Long = dao.insertContact(contact)

    suspend fun updateContact(contact: Contact) = dao.updateContact(contact)

    suspend fun deleteContact(contact: Contact) = dao.deleteContact(contact)

    suspend fun deleteContactsByIds(ids: List<Long>) = dao.deleteContactsByIds(ids)

    suspend fun getContactById(id: Long): Contact? = dao.getContactById(id)

    suspend fun getAllContactsSync(): List<Contact> = dao.getAllContactsSync()

    suspend fun getContactsByGroupSync(groupId: Long): List<Contact> =
        dao.getContactsByGroupSync(groupId)

    suspend fun getContactCountForGroup(groupId: Long): Int =
        dao.getContactCountForGroup(groupId)

    // ── Groups ──

    fun getAllGroups(): LiveData<List<Group>> = dao.getAllGroups()

    suspend fun getAllGroupsSync(): List<Group> = dao.getAllGroupsSync()

    suspend fun insertGroup(group: Group): Long = dao.insertGroup(group)

    suspend fun updateGroup(group: Group) = dao.updateGroup(group)

    suspend fun deleteGroup(group: Group) = dao.deleteGroup(group)

    suspend fun getGroupCount(): Int = dao.getGroupCount()

    suspend fun deleteContactsForGroup(groupId: Long) = dao.deleteContactsForGroup(groupId)

    suspend fun getGroupById(id: Long): Group? = dao.getGroupById(id)
}
