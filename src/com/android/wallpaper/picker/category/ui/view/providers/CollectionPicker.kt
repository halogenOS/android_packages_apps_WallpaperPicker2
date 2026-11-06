// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.view.providers

import androidx.fragment.app.Fragment
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.category.CategoryModel

/**
 * Creates the screen that shows a collection. Each picker is bound into a map keyed by the picker
 * type set in the `picker` attribute of a partner category.
 */
interface CollectionPicker {
    /** Returns the screen showing [category]. */
    operator fun invoke(category: CategoryModel): Fragment

    /**
     * Returns the screen showing [category] with [wallpaper] first, or null for a picker that opens
     * only at the start of a collection.
     */
    fun openAt(category: CategoryModel, wallpaper: WallpaperModel): Fragment? = null
}
