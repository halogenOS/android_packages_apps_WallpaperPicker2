// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.compose.theme.PlatformTheme
import com.android.wallpaper.R
import com.android.wallpaper.picker.SetWallpaperDialogFragment
import com.android.wallpaper.picker.category.ui.viewmodel.LiveCollectionArguments
import com.android.wallpaper.picker.category.ui.viewmodel.LiveCollectionViewModel
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity.Companion.returnToCustomizationPicker
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.util.ScreenSizeCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Shows the live wallpapers in a collection side by side, and sets the one chosen. */
@AndroidEntryPoint(Fragment::class)
class LiveCollectionFragment : Hilt_LiveCollectionFragment() {

    private val viewModel: LiveCollectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A dialog restored with this screen is missing its title and listener.
        (childFragmentManager.findFragmentByTag(DESTINATION_DIALOG_TAG)
                as? SetWallpaperDialogFragment)
            ?.let(::prepareDestinationDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()
                PlatformTheme {
                    LiveCollectionScreen(
                        state,
                        ScreenSizeCalculator.getInstance(),
                        onSetWallpaper = ::askDestination,
                    )
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.screenDone.collect { returnToCustomizationPicker(parentFragmentManager) }
            }
        }
    }

    private fun askDestination(wallpaper: LiveWallpaperModel) {
        viewModel.chooseForSetting(wallpaper)
        SetWallpaperDialogFragment()
            .also(::prepareDestinationDialog)
            .show(childFragmentManager, DESTINATION_DIALOG_TAG)
    }

    private fun prepareDestinationDialog(dialog: SetWallpaperDialogFragment) {
        dialog.setTitleResId(R.string.set_wallpaper_dialog_message)
        dialog.setListener { destination ->
            viewModel.setChosen(WallpaperDestination.fromDestinationInt(destination))
        }
    }

    companion object {
        private const val DESTINATION_DIALOG_TAG = "destination_dialog"

        /** Creates the screen for the collection set by [arguments]. */
        internal fun newInstance(arguments: LiveCollectionArguments) =
            LiveCollectionFragment().apply { this.arguments = arguments.toBundle() }
    }
}
