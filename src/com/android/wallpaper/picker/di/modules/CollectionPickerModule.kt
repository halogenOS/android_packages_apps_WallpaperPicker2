// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.di.modules

import com.android.wallpaper.picker.category.ui.view.providers.CollectionPicker
import com.android.wallpaper.picker.category.ui.view.providers.implementation.GridCollectionPicker
import com.android.wallpaper.picker.data.category.CollectionPickerTypes
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
abstract class CollectionPickerModule {

    @Binds
    @IntoMap
    @StringKey(GridCollectionPicker.TYPE)
    abstract fun bindGridCollectionPicker(impl: GridCollectionPicker): CollectionPicker

    companion object {
        @Provides
        fun provideCollectionPickerTypes(
            pickers: Map<String, @JvmSuppressWildcards Provider<CollectionPicker>>
        ) = CollectionPickerTypes(pickers.keys)
    }
}
