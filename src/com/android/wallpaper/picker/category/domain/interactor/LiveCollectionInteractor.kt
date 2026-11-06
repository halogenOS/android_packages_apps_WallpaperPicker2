// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.domain.interactor

import android.content.ComponentName
import android.stats.style.StyleEnums
import android.util.Log
import com.android.wallpaper.model.WallpaperModelsPair
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository2
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.service
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** The live wallpapers in a collection, where each is set, and setting one of them. */
internal class LiveCollectionInteractor
@Inject
constructor(
    private val categoryInteractor: CategoryInteractor,
    currentWallpapers: WallpaperRepository2,
    private val wallpaperRepository: WallpaperRepository,
) {

    /** Where each live wallpaper that is set shows, by the service that runs it. */
    val placements: Flow<Map<ComponentName, WallpaperDestination>> =
        currentWallpapers.currentWallpaperModels.map { it.livePlacements() }

    /** The live wallpapers in the collection with [collectionId], or null until it is loaded. */
    fun collection(collectionId: String): Flow<LiveCollection?> =
        category(collectionId).distinctUntilChanged().map { it?.toLiveCollection() }

    /**
     * Sets the wallpaper run by [service] in the collection with [collectionId] on [destination],
     * once the collection is loaded. Returns why it was not set, or null when it was.
     */
    suspend fun set(
        collectionId: String,
        service: ComponentName,
        destination: WallpaperDestination,
    ): SetFailure? {
        val collection = category(collectionId).filterNotNull().first()
        val wallpaper =
            collection.collectionCategoryData
                ?.wallpaperModels
                ?.filterIsInstance<LiveWallpaperModel>()
                ?.find { it.service == service } ?: return SetFailure.NOT_IN_COLLECTION
        return try {
            wallpaperRepository.setLiveWallpaper(
                setWallpaperEntryPoint = StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
                destination = destination,
                wallpaperModel = wallpaper,
            )
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cannot set $service on $destination", e)
            SetFailure.NOT_SET
        }
    }

    private fun category(collectionId: String): Flow<CategoryModel?> =
        categoryInteractor.categories.map { categories ->
            categories.find { it.commonCategoryData.collectionId == collectionId }
        }

    private fun CategoryModel.toLiveCollection(): LiveCollection {
        val collection = collectionCategoryData
        val models = collection?.wallpaperModels.orEmpty()
        models
            .filterNot { it is LiveWallpaperModel }
            .forEach {
                Log.w(
                    TAG,
                    "Skipping ${it.commonWallpaperData.id.wallpaperId} in " +
                        "${commonCategoryData.collectionId}, as it is not a live wallpaper",
                )
            }
        return LiveCollection(
            title = commonCategoryData.title,
            wallpapers = models.filterIsInstance<LiveWallpaperModel>(),
            featured = collection?.featuredWallpaper as? LiveWallpaperModel,
        )
    }

    private fun WallpaperModelsPair.livePlacements(): Map<ComponentName, WallpaperDestination> {
        val home = homeWallpaper.liveService
        val lock = wallpaperOnLockScreen.liveService
        return buildMap {
            home?.let { put(it, WallpaperDestination.HOME) }
            lock?.let {
                put(it, if (it == home) WallpaperDestination.BOTH else WallpaperDestination.LOCK)
            }
        }
    }

    private val WallpaperModel.liveService: ComponentName?
        get() = (this as? LiveWallpaperModel)?.service

    private companion object {
        const val TAG = "LiveCollectionInteractor"
    }
}

/** The live wallpapers in a collection, in its order, with the one its tile features. */
internal data class LiveCollection(
    val title: String,
    val wallpapers: List<LiveWallpaperModel>,
    val featured: LiveWallpaperModel?,
)

/** Why a wallpaper of a live collection was not set. */
internal enum class SetFailure {
    /** The wallpaper chosen is not in the collection anymore. */
    NOT_IN_COLLECTION,

    /** Setting the wallpaper failed. */
    NOT_SET,
}
