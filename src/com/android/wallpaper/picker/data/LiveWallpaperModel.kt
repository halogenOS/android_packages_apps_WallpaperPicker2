// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.data

import android.content.ComponentName
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel

/** The service that runs this wallpaper. */
internal val LiveWallpaperModel.service: ComponentName
    get() = liveWallpaperData.systemWallpaperInfo.component
