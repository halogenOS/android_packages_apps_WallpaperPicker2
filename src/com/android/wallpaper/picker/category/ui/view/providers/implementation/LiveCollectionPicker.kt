// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.view.providers.implementation

import android.content.ComponentName
import androidx.fragment.app.Fragment
import com.android.wallpaper.picker.category.ui.view.LiveCollectionFragment
import com.android.wallpaper.picker.category.ui.view.providers.CollectionPicker
import com.android.wallpaper.picker.category.ui.viewmodel.LiveCollectionArguments
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.service
import javax.inject.Inject

/** Opens a collection of live wallpapers as a carousel of their thumbnails. */
class LiveCollectionPicker @Inject constructor() : CollectionPicker {

    override fun invoke(category: CategoryModel): Fragment =
        createScreen(category, firstService = null)

    // The carousel shows only live wallpapers, so it cannot open at a static one.
    override fun openAt(category: CategoryModel, wallpaper: WallpaperModel): Fragment? =
        (wallpaper as? LiveWallpaperModel)?.let {
            createScreen(category, firstService = it.service)
        }

    private fun createScreen(category: CategoryModel, firstService: ComponentName?) =
        LiveCollectionFragment.newInstance(
            LiveCollectionArguments(category.commonCategoryData.collectionId, firstService)
        )

    companion object {
        /** The picker type set in a partner category to open collections in this picker. */
        const val TYPE = "live"
    }
}
