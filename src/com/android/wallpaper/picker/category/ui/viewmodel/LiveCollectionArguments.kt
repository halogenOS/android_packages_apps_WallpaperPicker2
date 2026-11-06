// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.viewmodel

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import java.io.Serializable

/** What the live collection screen is opened with. */
internal data class LiveCollectionArguments(val collectionId: String) : Serializable {

    /** The arguments for the fragment of the screen. */
    fun toBundle(): Bundle = bundleOf(KEY to this)

    companion object {
        private const val KEY = "live_collection"

        /** Reads the arguments that the screen was opened with. */
        fun from(handle: SavedStateHandle): LiveCollectionArguments =
            checkNotNull(handle.get<Any>(KEY) as? LiveCollectionArguments) {
                "The live collection screen was opened without its arguments"
            }
    }
}
