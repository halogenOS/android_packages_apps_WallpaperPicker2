// SPDX-FileCopyrightText: The halogenOS Project
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("ThumbnailImage")

package com.android.wallpaper.asset

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.OperationCanceledException
import android.util.Size
import androidx.annotation.WorkerThread
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.roundToInt

/**
 * How long reading a thumbnail image from another app may take. ContentResolver waits as long for a
 * provider whose process has to start first: 20 seconds for the provider to be published, and 3
 * seconds for the call (REMOTE_CONTENT_PROVIDER_TIMEOUT_MILLIS in android.content.ContentResolver,
 * checked 2026-11-07).
 */
const val THUMBNAIL_READ_DEADLINE_MILLIS = 23_000L

/** Any type, so that a provider without a type for the URI opens the file as well. */
private const val ANY_TYPE = "*/*"

private val deadlines = Handler(Looper.getMainLooper())

/**
 * Reads the image at [uri], provided by another app, for a thumbnail of [requested] pixels.
 *
 * A side of [requested] that is not positive, as for the original size in Glide, or that is longer
 * than the side of [display], is replaced by the side of [display]. The provider gets that size as
 * [ContentResolver.EXTRA_SIZE], and the image is decoded to fit inside it.
 *
 * @throws IOException when the image cannot be read, when it has more pixels than [display], or
 *   when reading takes longer than [THUMBNAIL_READ_DEADLINE_MILLIS]
 * @throws OperationCanceledException when [signal] is cancelled
 */
@WorkerThread
@Throws(IOException::class)
fun readThumbnailImage(
    resolver: ContentResolver,
    uri: Uri,
    requested: Size,
    display: Point,
    signal: CancellationSignal,
): Bitmap {
    val size = Size(requested.width.within(display.x), requested.height.within(display.y))
    val cancelAtDeadline = Runnable(signal::cancel)
    deadlines.postDelayed(cancelAtDeadline, THUMBNAIL_READ_DEADLINE_MILLIS)
    try {
        val options =
            Bundle().apply {
                putParcelable(ContentResolver.EXTRA_SIZE, Point(size.width, size.height))
            }
        val file =
            resolver.openTypedAssetFileDescriptor(uri, ANY_TYPE, options, signal)
                ?: throw FileNotFoundException("The provider of $uri returned no file")
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource { file }) { decoder, info, _ ->
            signal.throwIfCanceled()
            val image = info.size
            if (image.width.toLong() * image.height > display.x.toLong() * display.y) {
                throw IOException("The image at $uri has more pixels than the display: $image")
            }
            val scale =
                minOf(
                    1.0,
                    size.width.toDouble() / image.width,
                    size.height.toDouble() / image.height,
                )
            decoder.setTargetSize(
                (image.width * scale).roundToInt().coerceAtLeast(1),
                (image.height * scale).roundToInt().coerceAtLeast(1),
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (e: OperationCanceledException) {
        // The deadline is no longer queued once it has cancelled the signal.
        if (deadlines.hasCallbacks(cancelAtDeadline)) throw e
        throw IOException("Reading $uri took longer than $THUMBNAIL_READ_DEADLINE_MILLIS ms", e)
    } finally {
        deadlines.removeCallbacks(cancelAtDeadline)
    }
}

/** This side, or [displaySide] when this side is not positive or is longer. */
private fun Int.within(displaySide: Int) = if (this in 1..displaySide) this else displaySide
