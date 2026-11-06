// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.viewmodel

import android.content.ComponentName
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.picker.category.domain.interactor.LiveCollection
import com.android.wallpaper.picker.category.domain.interactor.LiveCollectionInteractor
import com.android.wallpaper.picker.category.domain.interactor.SetFailure
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.service
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The live wallpapers in one collection, where each is set, and setting one of them. */
@HiltViewModel
internal class LiveCollectionViewModel
@Inject
constructor(private val interactor: LiveCollectionInteractor, savedStateHandle: SavedStateHandle) :
    ViewModel() {

    private val arguments = LiveCollectionArguments.from(savedStateHandle)
    private val chosenService =
        savedStateHandle.getMutableStateFlow<ComponentName?>(KEY_CHOSEN_SERVICE, null)
    private val setting = MutableStateFlow<Setting>(Setting.Idle)
    private val wallpaperSet = Channel<Unit>(Channel.BUFFERED)

    val state: StateFlow<LiveCollectionState> =
        combine(
                interactor.collection(arguments.collectionId).filterNotNull(),
                interactor.placements,
                setting,
                ::toState,
            )
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), LiveCollectionState.Loading)

    /** Emits once a wallpaper is set, which closes the screen. */
    val screenDone: Flow<Unit> = wallpaperSet.receiveAsFlow()

    /** Stores [wallpaper] as the one to set once a destination is chosen for it. */
    fun chooseForSetting(wallpaper: LiveWallpaperModel) {
        chosenService.value = wallpaper.service
        setting.value = Setting.Idle
    }

    /** Sets the wallpaper stored by [chooseForSetting] on [destination]. */
    fun setChosen(destination: WallpaperDestination) {
        val service = chosenService.value
        if (service == null) {
            setting.value = Setting.Failed(SetFailure.NOT_SET)
            return
        }
        setting.value = Setting.InProgress
        viewModelScope.launch {
            when (val failure = interactor.set(arguments.collectionId, service, destination)) {
                null -> wallpaperSet.send(Unit)
                else -> setting.value = Setting.Failed(failure)
            }
        }
    }

    private fun toState(
        collection: LiveCollection,
        placements: Map<ComponentName, WallpaperDestination>,
        setting: Setting,
    ): LiveCollectionState {
        if (collection.wallpapers.isEmpty()) return LiveCollectionState.Empty(collection.title)
        val wallpapers = collection.wallpapers.map { LiveWallpaperItem(it, placements[it.service]) }
        val firstPage =
            wallpapers.indexOfFirst { it.placement != null }.takeIf { it >= 0 }
                ?: wallpapers.indexOfFirst { it.wallpaper == collection.featured }.coerceAtLeast(0)
        return LiveCollectionState.Shown(collection.title, wallpapers, firstPage, setting)
    }

    private companion object {
        const val KEY_CHOSEN_SERVICE = "chosen_service"
    }
}

/** What the live collection screen shows. */
internal sealed interface LiveCollectionState {
    /** The collection is not loaded yet. */
    data object Loading : LiveCollectionState

    /** The collection titled [title] holds no live wallpaper. */
    data class Empty(val title: String) : LiveCollectionState

    /** The wallpapers in the collection, starting at [firstPage]: the set one or the featured. */
    data class Shown(
        val title: String,
        val wallpapers: List<LiveWallpaperItem>,
        val firstPage: Int,
        val setting: Setting,
    ) : LiveCollectionState
}

/** Where setting the chosen wallpaper stands. */
internal sealed interface Setting {
    data object Idle : Setting

    data object InProgress : Setting

    data class Failed(val failure: SetFailure) : Setting
}

/** A wallpaper in the collection, with the place where it is set, or null if it is not set. */
internal data class LiveWallpaperItem(
    val wallpaper: LiveWallpaperModel,
    val placement: WallpaperDestination?,
)
