// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.util.extensions.android.view

import android.graphics.Point
import android.view.View
import com.android.wallpaper.util.ScreenSizeCalculator

/** Size of the display this attached view is on. */
internal fun View.displaySize(calculator: ScreenSizeCalculator): Point =
    calculator.getScreenSize(checkNotNull(display) { "The view is not attached" })
