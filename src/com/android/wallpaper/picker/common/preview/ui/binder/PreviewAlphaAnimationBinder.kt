/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.picker.common.preview.ui.binder

import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.picker.customization.ui.util.ViewAlphaAnimator.animateToAlpha
import com.android.wallpaper.picker.customization.ui.view.previewId
import com.android.wallpaper.picker.customization.ui.view.previewLabelContainerId
import com.android.wallpaper.picker.customization.ui.view.previewLabelId
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.preview.ui.view.ClickableMotionLayout
import kotlinx.coroutines.launch

/**
 * Animates the preview and the preview label to a target alpha. The timing to call bind() is
 * critical. Avoid calling it during Activity or Fragment transition that also takes care of the
 * alpha fade of the screen.
 */
object PreviewAlphaAnimationBinder {

    fun bind(
        previewPager: ClickableMotionLayout,
        viewModel: CustomizationPickerViewModel2,
        lifecycleOwner: LifecycleOwner,
    ) {
        val showDesktopUi =
            BaseFlags.get(previewPager.context).shouldShowDesktopUi(previewPager.context)

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pages.forEach { page ->
                    val preview: View = previewPager.requireViewById(page.previewId)
                    val previewLabel: TextView = previewPager.requireViewById(page.previewLabelId)
                    val previewLabelContainer: View =
                        previewPager.requireViewById(page.previewLabelContainerId)
                    val previewShade: View = preview.requireViewById(R.id.preview_shade)
                    launch {
                        viewModel.previewAlpha.getValue(page).collect {
                            (alpha, showLabel, shouldAnimate) ->
                            val shadeAlpha = 1 - alpha
                            val labelAlpha = if (showLabel) alpha else 0f
                            val labelVisibility = if (showLabel) View.VISIBLE else View.GONE
                            if (showDesktopUi) {
                                if (showLabel) {
                                    previewPager.addClickableViewId(page.previewLabelContainerId)
                                    previewLabelContainer.setOnClickListener {
                                        viewModel.selectPage(page)
                                    }
                                } else {
                                    previewPager.removeClickableViewId(page.previewLabelContainerId)
                                    previewLabelContainer.setOnClickListener(null)
                                }
                            }
                            if (shouldAnimate) {
                                previewLabelContainer.visibility = View.VISIBLE
                                previewLabelContainer.animateToAlpha(labelAlpha) {
                                    previewLabelContainer.visibility = labelVisibility
                                }
                                previewShade.animateToAlpha(shadeAlpha)
                            } else {
                                previewLabelContainer.alpha = labelAlpha
                                previewShade.alpha = shadeAlpha
                                previewLabelContainer.visibility = labelVisibility
                            }
                        }
                    }
                    launch {
                        viewModel.previewLabelTextAppearance.getValue(page).collect {
                            previewLabel.setTextAppearance(it)
                        }
                    }
                }
            }
        }
    }
}
