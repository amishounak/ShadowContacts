package com.shadowcontacts.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long = 1,
    val prefix: String = "",
    val name: String = "",
    val suffix: String = "",
    val phone: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Full display name: "Dr. John Smith, Jr." */
    fun displayName(): String {
        return buildString {
            if (prefix.isNotBlank()) append("$prefix ")
            append(name)
            if (suffix.isNotBlank()) append(", $suffix")
        }.trim()
    }
}
