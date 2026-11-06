// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.wallpaper.picker.category.ui.view

import android.view.View
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.compose.PlatformButton
import com.android.wallpaper.R
import com.android.wallpaper.picker.category.domain.interactor.SetFailure
import com.android.wallpaper.picker.category.ui.viewmodel.LiveCollectionState
import com.android.wallpaper.picker.category.ui.viewmodel.LiveWallpaperItem
import com.android.wallpaper.picker.category.ui.viewmodel.Setting
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.wallpapers.ui.view.compose.LayerListImage
import com.android.wallpaper.picker.wallpapers.ui.view.compose.LoadingSpinner
import com.android.wallpaper.picker.wallpapers.ui.view.compose.TopToolbar
import com.android.wallpaper.util.ScreenSizeCalculator
import com.android.wallpaper.util.extensions.android.view.displaySize

private val PAGE_SPACING = 16.dp
private val PEEK_WIDTH = 40.dp
private val DOT_SIZE = 6.dp
private val SELECTED_DOT_WIDTH = 18.dp
private val NAME_TOP_PADDING = 20.dp
private val DOTS_VERTICAL_PADDING = 16.dp
private val BUTTON_BOTTOM_PADDING = 24.dp
private val BUTTON_PROGRESS_SIZE = 18.dp
private val BUTTON_PROGRESS_SPACING = 8.dp
private val CARDS_TOP_PADDING = 8.dp
private val CHECK_MARK_PADDING = 12.dp
private val MESSAGE_PADDING = 16.dp

/** The carousel of a live collection, with the centered wallpaper ready to be set. */
@Composable
internal fun LiveCollectionScreen(
    state: LiveCollectionState,
    screenSizeCalculator: ScreenSizeCalculator,
    onSetWallpaper: (LiveWallpaperModel) -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                )
    ) {
        when (state) {
            LiveCollectionState.Loading -> LoadingSpinner(isLoading = true)
            is LiveCollectionState.Empty -> {
                TopToolbar(title = state.title, isRotationEnabled = false, startRotation = null)
                Message(R.string.live_collection_empty, Modifier.fillMaxSize())
            }
            is LiveCollectionState.Shown -> {
                TopToolbar(title = state.title, isRotationEnabled = false, startRotation = null)
                Carousel(state, screenSizeCalculator, onSetWallpaper)
            }
        }
    }
}

@Composable
private fun Carousel(
    state: LiveCollectionState.Shown,
    screenSizeCalculator: ScreenSizeCalculator,
    onSetWallpaper: (LiveWallpaperModel) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = state.firstPage) { state.wallpapers.size }
    val current = state.wallpapers.getOrNull(pagerState.currentPage)

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        WallpaperCards(
            state.wallpapers,
            pagerState,
            screenSizeCalculator,
            Modifier.weight(1f).fillMaxWidth(),
        )
        PageDots(
            pageCount = state.wallpapers.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(vertical = DOTS_VERTICAL_PADDING),
        )
        (state.setting as? Setting.Failed)?.let { Message(it.failure.message) }
        SetWallpaperButton(
            isSetting = state.setting == Setting.InProgress,
            onClick = current?.let { { onSetWallpaper(it.wallpaper) } },
            modifier = Modifier.padding(bottom = BUTTON_BOTTOM_PADDING),
        )
    }
}

/** The button that sets the centered wallpaper, showing progress while it is being set. */
@Composable
private fun SetWallpaperButton(
    isSetting: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    PlatformButton(
        onClick = { onClick?.invoke() },
        enabled = onClick != null && !isSetting,
        modifier = modifier,
    ) {
        if (isSetting) {
            CircularProgressIndicator(modifier = Modifier.size(BUTTON_PROGRESS_SIZE))
            Spacer(modifier = Modifier.width(BUTTON_PROGRESS_SPACING))
            Text(stringResource(R.string.set_wallpaper_progress_message))
        } else {
            Text(stringResource(R.string.set_wallpaper_button_text))
        }
    }
}

/** A line of text, centered in the space given to it. */
@Composable
private fun Message(@StringRes text: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(MESSAGE_PADDING),
        )
    }
}

/**
 * One card per wallpaper, shaped like the display and showing its thumbnail. The name of each
 * wallpaper and the screens it is set on appear under the card that shows it.
 */
@Composable
private fun WallpaperCards(
    wallpapers: List<LiveWallpaperItem>,
    pagerState: PagerState,
    screenSizeCalculator: ScreenSizeCalculator,
    modifier: Modifier = Modifier,
) {
    val displaySize = LocalView.current.displaySize(screenSizeCalculator)
    val cornerRadius = dimensionResource(R.dimen.grid_item_all_radius)
    val labelsHeight = labelsHeight()

    BoxWithConstraints(modifier.padding(top = CARDS_TOP_PADDING)) {
        val cardHeight: Dp =
            minOf(
                maxHeight - labelsHeight,
                (maxWidth - PEEK_WIDTH * 2) * displaySize.y / displaySize.x,
            )
        val cardWidth: Dp = cardHeight * displaySize.x / displaySize.y

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = (maxWidth - cardWidth) / 2),
            pageSpacing = PAGE_SPACING,
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val item = wallpapers[page]
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier.size(cardWidth, cardHeight).clip(RoundedCornerShape(cornerRadius))
                ) {
                    WallpaperThumbnail(item.wallpaper)
                    if (item.placement != null) {
                        LayerListImage(
                            id = R.drawable.wallpaper_check_circle_24dp,
                            modifier =
                                Modifier.align(Alignment.BottomEnd).padding(CHECK_MARK_PADDING),
                        )
                    }
                }
                WallpaperLabels(item, Modifier.size(cardWidth, labelsHeight))
            }
        }
    }
}

/** The name of a wallpaper and, for a wallpaper that is set, the screens it is set on. */
@Composable
private fun WallpaperLabels(item: LiveWallpaperItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = NAME_TOP_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = item.wallpaper.name(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        item.placement?.let {
            Text(
                text = stringResource(it.label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The height of [WallpaperLabels]: the space above the name, and one line for each label. */
@Composable
private fun labelsHeight(): Dp {
    val typography = MaterialTheme.typography
    return with(LocalDensity.current) {
        NAME_TOP_PADDING +
            typography.titleLarge.lineHeight.toDp() +
            typography.bodyMedium.lineHeight.toDp()
    }
}

/**
 * The thumbnail of a wallpaper, filling the card that shows it, or the placeholder when the
 * wallpaper has none.
 */
@Composable
private fun WallpaperThumbnail(wallpaper: LiveWallpaperModel) {
    val placeholderColor = colorResource(R.color.system_surface_bright).toArgb()
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        },
        update = { image ->
            wallpaper.commonWallpaperData.thumbAsset.loadDrawable(
                image.context,
                image,
                placeholderColor,
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/** A dot per page, the current one drawn wide. */
@Composable
private fun PageDots(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(DOT_SIZE),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            val isCurrent = page == currentPage
            val width by
                animateDpAsState(if (isCurrent) SELECTED_DOT_WIDTH else DOT_SIZE, label = "dot")
            Box(
                Modifier.size(width, DOT_SIZE)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

/**
 * The label declared by the service that runs the wallpaper. Partner categories hide wallpaper
 * titles in the grid, so [com.android.wallpaper.picker.data.CommonWallpaperData.title] is null for
 * the wallpapers listed in them.
 */
@Composable
private fun LiveWallpaperModel.name(): String {
    val packageManager = LocalContext.current.packageManager
    return remember(this, packageManager) {
        liveWallpaperData.systemWallpaperInfo.loadLabel(packageManager).toString()
    }
}

@get:StringRes
private val WallpaperDestination.label: Int
    get() =
        when (this) {
            WallpaperDestination.BOTH -> R.string.live_collection_set_on_both
            WallpaperDestination.HOME -> R.string.live_collection_set_on_home
            WallpaperDestination.LOCK -> R.string.live_collection_set_on_lock
        }

@get:StringRes
private val SetFailure.message: Int
    get() =
        when (this) {
            SetFailure.NOT_IN_COLLECTION -> R.string.live_collection_wallpaper_gone
            SetFailure.NOT_SET -> R.string.live_collection_set_failed
        }
