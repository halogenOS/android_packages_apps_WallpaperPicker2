// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.customization.ui

import com.android.wallpaper.picker.data.WallpaperModel

/** Opens the screens for wallpapers chosen on the main page. */
interface WallpaperNavigator {
    /**
     * Opens [wallpaper] in the picker set for the collection that contains it, or in the preview
     * screen when no such picker opens at a single wallpaper.
     */
    fun openWallpaper(wallpaper: WallpaperModel)
}
