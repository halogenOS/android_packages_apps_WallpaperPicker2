// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.data.category

import android.util.Log

/**
 * The picker types bound in this app. Partner categories come from another app, so a picker type
 * that is not bound here is read as none, and its collection opens in the stock grid.
 */
class CollectionPickerTypes(private val bound: Set<String>) {

    /** Returns [type] when a picker is bound for it, or null, logging a type that is not bound. */
    fun read(type: String?, collectionId: String): String? {
        if (type == null || type in bound) return type
        Log.w(TAG, "No picker \"$type\" for $collectionId, opening it in the grid")
        return null
    }

    private companion object {
        const val TAG = "CollectionPickerTypes"
    }
}
