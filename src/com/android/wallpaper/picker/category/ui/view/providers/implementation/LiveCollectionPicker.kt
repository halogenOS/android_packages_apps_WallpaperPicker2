// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.view.providers.implementation

import androidx.fragment.app.Fragment
import com.android.wallpaper.picker.category.ui.view.LiveCollectionFragment
import com.android.wallpaper.picker.category.ui.view.providers.CollectionPicker
import com.android.wallpaper.picker.category.ui.viewmodel.LiveCollectionArguments
import com.android.wallpaper.picker.data.category.CategoryModel
import javax.inject.Inject

/** Opens a collection of live wallpapers as a carousel of their thumbnails. */
class LiveCollectionPicker @Inject constructor() : CollectionPicker {

    override fun invoke(category: CategoryModel): Fragment =
        LiveCollectionFragment.newInstance(
            LiveCollectionArguments(category.commonCategoryData.collectionId)
        )

    companion object {
        /** The picker type set in a partner category to open collections in this picker. */
        const val TYPE = "live"
    }
}
