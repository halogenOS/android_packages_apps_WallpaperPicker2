// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("DefaultWallpaperOffer")

package com.android.wallpaper.model

import android.app.WallpaperManager
import android.content.Context
import com.android.wallpaper.module.PartnerProvider

/**
 * Whether the on-device collection offers [DefaultWallpaperInfo], the built-in image of the
 * framework. It does not when the partner hides it, or when the default wallpaper of the platform
 * is a live one, as the image is then not the default.
 */
fun isDefaultImageOffered(context: Context, partnerProvider: PartnerProvider): Boolean =
    !partnerProvider.shouldHideDefaultWallpaper() &&
        WallpaperManager.getCmfDefaultWallpaperComponent(context) == null
