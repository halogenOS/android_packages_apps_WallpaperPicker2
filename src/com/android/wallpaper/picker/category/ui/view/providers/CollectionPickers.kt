// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.view.providers

import com.android.wallpaper.picker.category.ui.view.providers.implementation.GridCollectionPicker
import com.android.wallpaper.picker.data.category.CategoryModel
import javax.inject.Inject
import javax.inject.Singleton

/** Finds the [CollectionPicker] set on a collection, using the stock grid for every other one. */
@Singleton
class CollectionPickers
@Inject
constructor(private val pickers: Map<String, @JvmSuppressWildcards CollectionPicker>) {

    private val grid = pickers.getValue(GridCollectionPicker.TYPE)

    fun findPickerFor(category: CategoryModel): CollectionPicker =
        category.collectionCategoryData?.pickerType?.let(pickers::getValue) ?: grid
}
