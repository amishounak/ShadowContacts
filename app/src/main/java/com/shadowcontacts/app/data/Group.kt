package com.shadowcontacts.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "All Contacts",
    val isDefault: Boolean = false
)
