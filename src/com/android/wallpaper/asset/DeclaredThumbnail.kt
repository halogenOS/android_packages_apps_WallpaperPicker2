// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("DeclaredThumbnail")

package com.android.wallpaper.asset

import android.app.WallpaperInfo
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/** Name of the meta-data that contains the thumbnail URI, see [findDeclaredThumbnailUri]. */
private const val THUMBNAIL_URI_META_DATA = "com.android.wallpaper.thumbnail_uri"

private const val TAG = "DeclaredThumbnail"

/**
 * Returns the thumbnail URI declared by the service of [info], or null when it declares none, or
 * when the authority of the URI does not match a provider in the same package as the wallpaper.
 *
 * A live wallpaper service declares the URI in its meta-data `com.android.wallpaper.thumbnail_uri`.
 * The picker then shows the image at that URI as the thumbnail of the wallpaper, and the thumbnail
 * resource of the wallpaper when the image cannot be read. The URI and its provider must meet this
 * contract:
 * - The URI has the `content` scheme and the authority of a provider in the same package as the
 *   wallpaper.
 * - A caller with `android.permission.SET_WALLPAPER_COMPONENT`, which the picker needs to set live
 *   wallpapers, can read it.
 * - [android.content.ContentProvider.openTypedAssetFile] returns the image rendered at the size
 *   given as [ContentResolver.EXTRA_SIZE], the size in pixels at which the picker shows it. An
 *   image with more pixels than the display fails to load.
 * - The picker reads the image again each time it shows the thumbnail, so the image can change, for
 *   example with the theme colors.
 */
fun findDeclaredThumbnailUri(packageManager: PackageManager, info: WallpaperInfo): Uri? {
    val declared = info.serviceInfo.metaData?.getString(THUMBNAIL_URI_META_DATA) ?: return null
    val uri = Uri.parse(declared)
    val provider =
        uri.authority
            ?.takeIf { uri.scheme == ContentResolver.SCHEME_CONTENT }
            ?.let {
                packageManager.resolveContentProvider(it, PackageManager.ComponentInfoFlags.of(0))
            }
    if (provider?.packageName != info.packageName) {
        Log.w(
            TAG,
            "${info.component} declares the thumbnail $uri, whose authority does not match a " +
                "provider in the same package",
        )
        return null
    }
    return uri
}
