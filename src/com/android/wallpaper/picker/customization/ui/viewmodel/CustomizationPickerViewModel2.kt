/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.wallpaper.picker.customization.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.PickerPage
import com.android.wallpaper.picker.common.preview.ui.viewmodel.BasePreviewViewModel
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import com.android.wallpaper.util.ActivityUtils
import com.android.wallpaper.util.LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CustomizationPickerViewModel2
@Inject
constructor(
    @ApplicationContext private val context: Context,
    customizationOptionUtil: CustomizationOptionUtil,
    customizationOptionsViewModelFactory: CustomizationOptionsViewModelFactory,
    basePreviewViewModelFactory: BasePreviewViewModel.Factory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * Data class defining the target alpha that a preview should be set to. If shouldAnimate is
     * true, the preview will animate to the target alpha value.
     */
    data class PreviewAlpha(val alpha: Float, val showLabel: Boolean, val shouldAnimate: Boolean)

    private val initialDestination: String? = savedStateHandle[KEY_DESTINATION]
    private val initialShortcutSlotId: String? = savedStateHandle[KEY_SHORTCUT_SLOT_ID]
    private val launchSource: String? = savedStateHandle[WALLPAPER_LAUNCH_SOURCE]

    val customizationOptionsViewModel =
        customizationOptionsViewModelFactory.create(
            viewModelScope = viewModelScope,
            initialDeepLinkDestination = initialDestination,
            initialDeepLinkShortcutSlotId = initialShortcutSlotId,
        )
    val basePreviewViewModel = basePreviewViewModelFactory.create(viewModelScope)

    enum class PickerScreen {
        MAIN,
        CUSTOMIZATION_OPTION,
    }

    /** Pages the picker shows. */
    val pages: Set<PickerPage> = customizationOptionUtil.pages

    private val initialPage =
        initialDestination
            ?.let { customizationOptionUtil.getCustomizationOptionFromDestination(it) }
            ?.let { customizationOptionUtil.getPageFromOption(it) }
    private val isLaunchedFromLauncher = ActivityUtils.isLaunchedFromLauncher(launchSource)
    private val _selectedPage =
        MutableStateFlow(
            initialPage
                ?: if (
                    isLaunchedFromLauncher || BaseFlags.get(context).shouldShowDesktopUi(context)
                )
                    PickerPage.HOME_SCREEN
                else PickerPage.LOCK_SCREEN
        )
    val selectedPage = _selectedPage.asStateFlow()

    /** Screen shown by the preview of the selected page. Wallpaper changes apply to this screen. */
    val selectedPreviewScreen =
        selectedPage
            .map { it.previewScreen }
            .stateIn(viewModelScope, SharingStarted.Eagerly, selectedPage.value.previewScreen)

    fun selectPage(page: PickerPage) {
        _selectedPage.value = page
    }

    val screen =
        customizationOptionsViewModel.selectedOption
            .map {
                if (it != null) {
                    Pair(PickerScreen.CUSTOMIZATION_OPTION, it)
                } else {
                    Pair(PickerScreen.MAIN, null)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                Pair(PickerScreen.MAIN, null),
            )

    private val previewReadiness: Map<PickerPage, MutableStateFlow<Boolean>> =
        PickerPage.entries.associateWith { MutableStateFlow(false) }

    fun setPreviewReady(page: PickerPage, isReady: Boolean) {
        previewReadiness.getValue(page).value = isReady
    }

    /** Flows that emit to trigger the preview of each page to animate to an alpha value. */
    val previewAlpha: Map<PickerPage, Flow<PreviewAlpha>> =
        PickerPage.entries.associateWith { page ->
            combine(previewReadiness.getValue(page), screen, selectedPage) {
                    isPreviewReady,
                    navigationScreen,
                    currentPage ->
                    getPreviewAlpha(
                        isPreviewReady = isPreviewReady,
                        navigationScreen = navigationScreen.first,
                        selectedPage = currentPage,
                        targetPage = page,
                    )
                }
                .distinctUntilChanged()
                .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
        }

    /** Flows of a style res Id that represents the text appearance of the label of each page. */
    val previewLabelTextAppearance: Map<PickerPage, Flow<Int>> =
        PickerPage.entries.associateWith { page ->
            selectedPage.map {
                if (it == page) {
                    R.style.TextAppearance_Preview_Label_Selected
                } else {
                    R.style.TextAppearance_Preview_Label_Unselected
                }
            }
        }

    /**
     * Get the preview's target alpha value to animate or set to.
     *
     * @return [PreviewAlpha] contains the target alpha value and shouldAnimate. If shouldAnimate is
     *   true, the view should animate to the target alpha; otherwise, directly set to the alpha.
     */
    private fun getPreviewAlpha(
        isPreviewReady: Boolean,
        navigationScreen: PickerScreen,
        selectedPage: PickerPage,
        targetPage: PickerPage,
    ): PreviewAlpha {
        return if (isPreviewReady) {
            when (navigationScreen) {
                PickerScreen.MAIN ->
                    if (selectedPage == targetPage)
                        PreviewAlpha(
                            alpha = PREVIEW_SHOW_ALPHA,
                            showLabel = true,
                            shouldAnimate = true,
                        )
                    else
                        PreviewAlpha(
                            alpha = PREVIEW_FADE_ALPHA,
                            showLabel = true,
                            shouldAnimate = true,
                        )
                PickerScreen.CUSTOMIZATION_OPTION -> {
                    when (selectedPage) {
                        targetPage ->
                            PreviewAlpha(
                                alpha = PREVIEW_SHOW_ALPHA,
                                showLabel = false,
                                shouldAnimate = true,
                            )
                        else ->
                            PreviewAlpha(
                                alpha = PREVIEW_HIDE_ALPHA,
                                showLabel = false,
                                shouldAnimate = true,
                            )
                    }
                }
            }
        } else {
            PreviewAlpha(alpha = PREVIEW_HIDE_ALPHA, showLabel = false, shouldAnimate = false)
        }
    }

    val isPagerInteractable: Flow<Boolean> =
        customizationOptionsViewModel.selectedOption.map { it == null }

    val isPreviewClickable: Flow<Boolean> =
        combine(basePreviewViewModel.wallpapers, isPagerInteractable) {
            wallpapers,
            isPagerInteractable ->
            wallpapers != null && isPagerInteractable
        }

    companion object {
        const val PREVIEW_SHOW_ALPHA = 1F
        const val PREVIEW_HIDE_ALPHA = 0F
        const val PREVIEW_FADE_ALPHA = 0.4F

        const val KEY_DESTINATION = "destination"
        const val KEY_SHORTCUT_SLOT_ID = "slot_id"
    }
}
