// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.view.providers.implementation

import androidx.fragment.app.Fragment
import com.android.wallpaper.picker.category.ui.view.providers.CollectionPicker
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.wallpapers.data.repository.CategoryWallpapersRepository
import com.android.wallpaper.picker.wallpapers.ui.view.CategoryWallpapersFragment
import javax.inject.Inject

/** Opens a collection in the stock grid of [CategoryWallpapersFragment]. */
class GridCollectionPicker
@Inject
constructor(private val categoryWallpapersRepository: CategoryWallpapersRepository) :
    CollectionPicker {

    override fun invoke(category: CategoryModel): Fragment {
        categoryWallpapersRepository.setSelectedCategory(category = category)
        return CategoryWallpapersFragment()
    }

    companion object {
        /** Key for the stock grid, which opens every collection not set to another picker type. */
        const val TYPE = "grid"
    }
}
