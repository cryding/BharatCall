package com.bharatcall.app

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.net.Uri

object CallerLookup {
    fun findContactName(context: Context, phoneNumber: String): String? {
        if (phoneNumber.isBlank() || context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        return context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }
}
