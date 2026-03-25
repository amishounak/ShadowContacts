package com.shadowcontacts.app.utils

import android.content.Context
import android.provider.ContactsContract

object PhoneContactImporter {

    data class PhoneContactEntry(
        val prefix: String,
        val name: String,
        val suffix: String,
        val phone: String
    )

    /**
     * Read all contacts from the phone's contact book.
     * Returns a list of PhoneContactEntry with structured name + primary phone.
     */
    fun readPhoneContacts(context: Context): List<PhoneContactEntry> {
        val contactMap = mutableMapOf<Long, PhoneContactEntry>()

        // Step 1: Read structured names
        val nameCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredName.PREFIX,
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.StructuredName.SUFFIX
            ),
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
            null
        )

        nameCursor?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val prefixIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
            val suffixIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIdx)
                val prefix = cursor.getString(prefixIdx) ?: ""
                val name = cursor.getString(nameIdx) ?: ""
                val suffix = cursor.getString(suffixIdx) ?: ""

                if (name.isNotBlank()) {
                    contactMap[contactId] = PhoneContactEntry(prefix, name, suffix, "")
                }
            }
        }

        // Step 2: Read phone numbers
        val phoneCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        phoneCursor?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIdx)
                val number = cursor.getString(numIdx) ?: ""

                val existing = contactMap[contactId]
                if (existing != null && existing.phone.isBlank() && number.isNotBlank()) {
                    contactMap[contactId] = existing.copy(phone = number)
                }
            }
        }

        return contactMap.values.toList()
    }
}
