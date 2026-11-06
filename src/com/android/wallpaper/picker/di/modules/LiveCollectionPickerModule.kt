// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.di.modules

import com.android.wallpaper.picker.category.ui.view.providers.CollectionPicker
import com.android.wallpaper.picker.category.ui.view.providers.implementation.LiveCollectionPicker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
abstract class LiveCollectionPickerModule {

    @Binds
    @IntoMap
    @StringKey(LiveCollectionPicker.TYPE)
    abstract fun bindLiveCollectionPicker(impl: LiveCollectionPicker): CollectionPicker
}
