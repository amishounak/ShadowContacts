package com.shadowcontacts.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ContactDao {

    // ── Contacts ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts WHERE id IN (:ids)")
    suspend fun deleteContactsByIds(ids: List<Long>)

    @Query("SELECT * FROM contacts WHERE groupId = :groupId ORDER BY name COLLATE NOCASE ASC")
    fun getContactsByGroup(groupId: Long): LiveData<List<Contact>>

    @Query("SELECT * FROM contacts WHERE groupId = :groupId AND (name LIKE '%' || :query || '%' OR prefix LIKE '%' || :query || '%' OR suffix LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY name COLLATE NOCASE ASC")
    fun searchContacts(groupId: Long, query: String): LiveData<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): Contact?

    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllContactsSync(): List<Contact>

    @Query("SELECT * FROM contacts WHERE groupId = :groupId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getContactsByGroupSync(groupId: Long): List<Contact>

    @Query("SELECT COUNT(*) FROM contacts WHERE groupId = :groupId")
    suspend fun getContactCountForGroup(groupId: Long): Int

    // ── Groups ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Update
    suspend fun updateGroup(group: Group)

    @Delete
    suspend fun deleteGroup(group: Group)

    @Query("SELECT * FROM groups ORDER BY id ASC")
    fun getAllGroups(): LiveData<List<Group>>

    @Query("SELECT * FROM groups ORDER BY id ASC")
    suspend fun getAllGroupsSync(): List<Group>

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun getGroupCount(): Int

    @Query("DELETE FROM contacts WHERE groupId = :groupId")
    suspend fun deleteContactsForGroup(groupId: Long)

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Long): Group?
}
