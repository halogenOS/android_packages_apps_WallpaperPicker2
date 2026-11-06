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

package com.android.wallpaper.picker.customization.ui.binder

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.picker.icon.ui.util.IconStyleViewUtil
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.PickerPage
import com.android.wallpaper.model.Screen
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.customization.shared.model.CategoryType
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity
import com.android.wallpaper.picker.customization.ui.WallpaperNavigator
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.view.PackThemeSuggestedChip
import com.android.wallpaper.picker.customization.ui.view.WallpaperPickerEntry
import com.android.wallpaper.picker.customization.ui.view.optionContainerId
import com.android.wallpaper.picker.customization.ui.view.selectedStateId
import com.android.wallpaper.picker.customization.ui.view.showsWallpaperEntry
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsData
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.CUSTOMIZATION_OPTION
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.MAIN
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.preview.ui.view.ClickableMotionLayout
import com.android.wallpaper.util.CuratedPhotosTimeUtil
import kotlinx.coroutines.launch

object CustomizationPickerBinder2 {
    /**
     * @return Callback for the [CustomizationPickerActivity] to set
     *   [CustomizationPickerViewModel2]'s screen state to null, which infers to the main screen. We
     *   need this callback to handle the back navigation in [CustomizationPickerActivity].
     */
    fun bind(
        customizationOptionsData: CustomizationOptionsData,
        view: View,
        optionEntries: Map<PickerPage, List<Pair<CustomizationOption, View>>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        customizationOptionsBinder: CustomizationOptionsBinder,
        lifecycleOwner: LifecycleOwner,
        navigateToPrimary: () -> Unit,
        navigateToSecondary: (option: CustomizationOption) -> Unit,
        navigateToWallpaperCategoriesScreen: (screen: Screen) -> Unit,
        navigateToMoreLockScreenSettingsActivity: () -> Unit,
        navigateToColorContrastSettingsActivity: () -> Unit,
        navigateToLockScreenNotificationsSettingsActivity: () -> Unit,
        navigateToPreviewScreen:
            ((wallpaperModel: WallpaperModel, setWallpaperEntryPoint: Int) -> Unit)?,
        navigateToPackThemeActivity: (Intent) -> Unit,
        navigateToScreenSaverSettingsActivity: () -> Unit,
        navigateToWallpaperCollectionScreen:
            ((categoryModel: CategoryModel, categoryType: CategoryType) -> Unit)?,
        navigateToExtendedWallpaperEffects: (() -> Unit)?,
        wallpaperNavigator: WallpaperNavigator,
        packThemeSuggestedChip: PackThemeSuggestedChip?,
        packThemeSuggestedEntryBinder: PackThemeSuggestedEntryBinder,
        curatedPhotosTimeUtil: CuratedPhotosTimeUtil,
        userEventLogger: UserEventLogger,
        iconStyleViewUtil: IconStyleViewUtil,
        launchFontFilePicker: ((onResult: (List<android.net.Uri>) -> Unit) -> Unit)? = null,
    ) {
        val optionContainers: Map<PickerPage, LinearLayout> =
            viewModel.pages.associateWith { view.requireViewById(it.optionContainerId) }
        val wallpaperPickerEntry: WallpaperPickerEntry =
            view.requireViewById(R.id.wallpaper_picker_entry)
        val previewPager: ClickableMotionLayout = view.requireViewById(R.id.preview_pager)

        previewPager.setOnTransitionCompleted { currentId ->
            viewModel.pages
                .firstOrNull { it.selectedStateId == currentId }
                ?.let(viewModel::selectPage)
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.screen.collect { (screen, option) ->
                        when (screen) {
                            MAIN -> {
                                navigateToPrimary()
                            }
                            CUSTOMIZATION_OPTION -> {
                                option?.let(navigateToSecondary)
                            }
                        }
                    }
                }

                launch {
                    viewModel.selectedPage.collect { selectedPage ->
                        if (previewPager.currentState != selectedPage.selectedStateId) {
                            previewPager.jumpToState(selectedPage.selectedStateId)
                        }
                        optionContainers.forEach { (page, container) ->
                            container.isInvisible = page != selectedPage
                        }
                        wallpaperPickerEntry.isInvisible = !selectedPage.showsWallpaperEntry
                    }
                }
            }
        }

        if (BaseFlags.get(view.context).isPackThemeEnabled()) {
            packThemeSuggestedChip?.let {
                packThemeSuggestedEntryBinder.bind(
                    view = it,
                    viewModel = viewModel,
                    colorUpdateViewModel = colorUpdateViewModel,
                    lifecycleOwner = lifecycleOwner,
                    navigateToPackThemeActivity = navigateToPackThemeActivity,
                )
            }
        }

        WallpaperPickerEntryBinder.bind(
            view = wallpaperPickerEntry,
            viewModel = viewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            lifecycleOwner = lifecycleOwner,
            navigateToWallpaperCategoriesScreen = navigateToWallpaperCategoriesScreen,
            navigateToPreviewScreen = navigateToPreviewScreen,
            navigateToWallpaperCollectionScreen = navigateToWallpaperCollectionScreen,
            navigateToExtendedWallpaperEffects = navigateToExtendedWallpaperEffects,
            wallpaperNavigator = wallpaperNavigator,
            curatedPhotosTimeUtil = curatedPhotosTimeUtil,
            userEventLogger = userEventLogger,
        )

        customizationOptionsBinder.bind(
            customizationOptionsData,
            view,
            optionEntries,
            customizationOptionFloatingSheetViewMap,
            viewModel,
            colorUpdateViewModel,
            lifecycleOwner,
            navigateToMoreLockScreenSettingsActivity,
            navigateToColorContrastSettingsActivity,
            navigateToLockScreenNotificationsSettingsActivity,
            navigateToPackThemeActivity,
            navigateToScreenSaverSettingsActivity,
            iconStyleViewUtil,
            launchFontFilePicker,
        )
    }
}
