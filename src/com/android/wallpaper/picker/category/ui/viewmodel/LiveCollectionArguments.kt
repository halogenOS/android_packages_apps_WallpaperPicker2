// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.viewmodel

import android.content.ComponentName
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle

/**
 * What the live collection screen is opened with: the collection, and the service that runs the
 * wallpaper shown first, or null to show the set one or the featured one first.
 */
internal data class LiveCollectionArguments(
    val collectionId: String,
    val firstService: ComponentName?,
) {

    /** The arguments for the fragment of the screen. */
    fun toBundle(): Bundle =
        bundleOf(KEY_COLLECTION to collectionId, KEY_FIRST_SERVICE to firstService)

    companion object {
        private const val KEY_COLLECTION = "live_collection"
        private const val KEY_FIRST_SERVICE = "live_collection_first_service"

        /** Reads the arguments that the screen was opened with. */
        fun from(handle: SavedStateHandle): LiveCollectionArguments =
            LiveCollectionArguments(
                collectionId =
                    checkNotNull(handle.get<String>(KEY_COLLECTION)) {
                        "The live collection screen was opened without its collection"
                    },
                firstService = handle.get<ComponentName>(KEY_FIRST_SERVICE),
            )
    }
}
