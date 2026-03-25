package com.shadowcontacts.app.utils

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shadowcontacts.app.data.Contact
import com.shadowcontacts.app.data.ContactDatabase
import com.shadowcontacts.app.data.Group
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImportExportHelper {

    private val gson = Gson()

    data class BackupData(
        val appName: String = "ShadowContacts",
        val version: Int = 1,
        val exportedAt: Long = System.currentTimeMillis(),
        val groups: List<GroupBackup>,
        val contacts: List<ContactBackup>
    )

    data class GroupBackup(
        val id: Long,
        val name: String,
        val isDefault: Boolean
    )

    data class ContactBackup(
        val id: Long,
        val groupId: Long,
        val prefix: String,
        val name: String,
        val suffix: String,
        val phone: String,
        val description: String,
        val createdAt: Long,
        val updatedAt: Long
    )

    suspend fun export(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val dao = ContactDatabase.getDatabase(context).contactDao()
            val groups = dao.getAllGroupsSync()
            val contacts = dao.getAllContactsSync()

            val backup = BackupData(
                groups = groups.map { GroupBackup(it.id, it.name, it.isDefault) },
                contacts = contacts.map {
                    ContactBackup(
                        it.id, it.groupId, it.prefix, it.name, it.suffix,
                        it.phone, it.description, it.createdAt, it.updatedAt
                    )
                }
            )

            val json = gson.toJson(backup)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            }

            Result.success(contacts.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun import(context: Context, uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return@withContext Result.failure(Exception("Cannot read file"))

            val backup = gson.fromJson(json, BackupData::class.java)
                ?: return@withContext Result.failure(Exception("Invalid backup format"))

            val dao = ContactDatabase.getDatabase(context).contactDao()
            val existingGroups = dao.getAllGroupsSync()
            val existingContacts = dao.getAllContactsSync()

            // Build group ID mapping: backup groupId -> new groupId
            val groupIdMap = mutableMapOf<Long, Long>()
            var groupsImported = 0

            for (bg in backup.groups) {
                // Check if a group with the same name exists
                val existing = existingGroups.find { it.name == bg.name }
                if (existing != null) {
                    groupIdMap[bg.id] = existing.id
                } else {
                    val newId = dao.insertGroup(Group(name = bg.name, isDefault = false))
                    groupIdMap[bg.id] = newId
                    groupsImported++
                }
            }

            // Import contacts, skipping duplicates
            var contactsImported = 0
            var duplicatesSkipped = 0

            for (bc in backup.contacts) {
                val targetGroupId = groupIdMap[bc.groupId] ?: continue

                // Duplicate = same name + phone in same group
                val isDuplicate = existingContacts.any { existing ->
                    existing.name.equals(bc.name, ignoreCase = true) &&
                        existing.phone == bc.phone &&
                        existing.groupId == targetGroupId
                }

                if (isDuplicate) {
                    duplicatesSkipped++
                } else {
                    dao.insertContact(
                        Contact(
                            groupId = targetGroupId,
                            prefix = bc.prefix,
                            name = bc.name,
                            suffix = bc.suffix,
                            phone = bc.phone,
                            description = bc.description,
                            createdAt = bc.createdAt,
                            updatedAt = bc.updatedAt
                        )
                    )
                    contactsImported++
                }
            }

            Result.success(ImportResult(groupsImported, contactsImported, duplicatesSkipped))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class ImportResult(
        val groupsImported: Int,
        val contactsImported: Int,
        val duplicatesSkipped: Int
    )
}
