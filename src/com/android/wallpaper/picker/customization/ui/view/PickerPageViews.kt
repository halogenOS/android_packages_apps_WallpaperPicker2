// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.customization.ui.view

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.android.wallpaper.R
import com.android.wallpaper.model.PickerPage

/** State of the preview pager motion scene that centers the preview of the page. */
@get:IdRes
val PickerPage.selectedStateId: Int
    get() =
        when (this) {
            PickerPage.LOCK_SCREEN -> R.id.lock_preview_selected
            PickerPage.HOME_SCREEN -> R.id.home_preview_selected
            PickerPage.STYLE -> R.id.style_preview_selected
        }

/** Swipe transition of the preview pager that ends at the page, null for the leftmost page. */
@get:IdRes
val PickerPage.swipeTransitionId: Int?
    get() =
        when (this) {
            PickerPage.LOCK_SCREEN -> null
            PickerPage.HOME_SCREEN -> R.id.preview_swipe_transition
            PickerPage.STYLE -> R.id.style_swipe_transition
        }

/** Container that lists the customization options of the page. */
@get:IdRes
val PickerPage.optionContainerId: Int
    get() =
        when (this) {
            PickerPage.LOCK_SCREEN -> R.id.lock_customization_option_container
            PickerPage.HOME_SCREEN -> R.id.home_customization_option_container
            PickerPage.STYLE -> R.id.style_customization_option_container
        }

/** Preview card of the page in the preview pager. */
@get:IdRes
val PickerPage.previewId: Int
    get() =
        when (this) {
            PickerPage.LOCK_SCREEN -> R.id.lock_preview
            PickerPage.HOME_SCREEN -> R.id.home_preview
            PickerPage.STYLE -> R.id.style_preview
        }

/** Label above the preview card of the page. */
@get:IdRes
val PickerPage.previewLabelId: Int
    get() =
        when (this) {
            PickerPage.LOCK_SCREEN -> R.id.lock_preview_label
            PickerPage.HOME_SCREEN -> R.id.home_preview_label
            PickerPage.STYLE -> R.id.style_preview_label
        }

/** Container around the preview label of the page. */
@get:IdRes
val PickerPage.previewLabelContainerId: Int
    get() =
        when (this) {
            PickerPage.LOCK_SCREEN -> R.id.lock_preview_label_container
            PickerPage.HOME_SCREEN -> R.id.home_preview_label_container
            PickerPage.STYLE -> R.id.style_preview_label_container
        }

/** Name of the page in its label and in the description of its preview. */
@get:StringRes
val PickerPage.titleRes: Int
    get() =
        when (this) {
            PickerPage.LOCK_SCREEN -> R.string.lock_screen_tab
            PickerPage.HOME_SCREEN -> R.string.home_screen_tab
            PickerPage.STYLE -> R.string.style_tab
        }

/** Whether the page shows the wallpaper entry above its options. */
val PickerPage.showsWallpaperEntry: Boolean
    get() =
        when (this) {
            PickerPage.LOCK_SCREEN,
            PickerPage.HOME_SCREEN -> true
            PickerPage.STYLE -> false
        }
