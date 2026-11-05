// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.model

/** Pages of the customization picker, each with the screen its preview shows. */
enum class PickerPage(val previewScreen: Screen) {
    LOCK_SCREEN(Screen.LOCK_SCREEN),
    HOME_SCREEN(Screen.HOME_SCREEN),
    /** Style options supplied by the app that includes the picker, previewed on the home screen. */
    STYLE(Screen.HOME_SCREEN),
}
