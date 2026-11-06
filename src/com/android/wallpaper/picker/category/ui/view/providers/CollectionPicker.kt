// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.view.providers

import androidx.fragment.app.Fragment
import com.android.wallpaper.picker.data.category.CategoryModel

/**
 * Creates the screen that shows a collection. Each picker is bound into a map keyed by the picker
 * type set in the `picker` attribute of a partner category.
 */
interface CollectionPicker {
    /** Returns the screen showing [category]. */
    operator fun invoke(category: CategoryModel): Fragment

    companion object {
        /** Key of the stock grid, which opens every collection that names no other picker. */
        const val GRID = "grid"
    }
}
