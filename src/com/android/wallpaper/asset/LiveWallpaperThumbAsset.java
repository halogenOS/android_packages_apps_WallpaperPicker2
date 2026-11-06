/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.wallpaper.asset;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.android.wallpaper.module.DrawableLayerResolver;
import com.android.wallpaper.module.InjectorProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Asset wrapping a drawable for a live wallpaper thumbnail.
 */
public class LiveWallpaperThumbAsset extends Asset {
    private static final String TAG = "LiveWallpaperThumbAsset";
    private static final ExecutorService sExecutorService = Executors.newCachedThreadPool();
    // The deadline of a read from a URI, and then the two seconds for the thumbnail resource.
    private static final long LOW_RES_THUMB_TIMEOUT_MILLIS =
            ThumbnailImage.THUMBNAIL_READ_DEADLINE_MILLIS + TimeUnit.SECONDS.toMillis(2);
    // The size of the original image, at most the size of the display.
    private static final Size ORIGINAL_SIZE = new Size(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);

    protected final Context mContext;
    protected final android.app.WallpaperInfo mInfo;
    protected final DrawableLayerResolver mLayerResolver;
    // The content Uri of thumbnail
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public Uri mUri;
    private Drawable mCachedThumbnail;

    /**
     * The thumbnail declared by the wallpaper: the image at its declared thumbnail URI, else its
     * thumbnail resource.
     */
    public LiveWallpaperThumbAsset(Context context, android.app.WallpaperInfo info) {
        this(context, info,
                DeclaredThumbnail.findDeclaredThumbnailUri(context.getPackageManager(), info));
    }

    public LiveWallpaperThumbAsset(Context context, android.app.WallpaperInfo info, Uri uri) {
        mContext = context.getApplicationContext();
        mInfo = info;
        mUri = uri;
        mLayerResolver = InjectorProvider.getInjector().getDrawableLayerResolver();
    }

    @Override
    public void decodeBitmap(int targetWidth, int targetHeight, boolean useHardwareBitmapIfPossible,
                             BitmapReceiver receiver) {
        sExecutorService.execute(() -> {
            Drawable thumb = getThumbnailDrawable(new Size(targetWidth, targetHeight),
                    new CancellationSignal());

            // Live wallpaper components may or may not specify a thumbnail drawable.
            if (thumb instanceof BitmapDrawable) {
                BitmapDrawable drawableThumb = (BitmapDrawable) thumb;
                int thumbHeight = drawableThumb.getIntrinsicHeight();
                int thumbWidth = drawableThumb.getIntrinsicWidth();
                int height, width;
                if (thumbHeight > 0 && thumbWidth > 0) {
                    double ratio = thumbHeight > thumbWidth ? (double) targetHeight / thumbHeight
                            : (double) targetWidth / thumbWidth;
                    height = (int) (thumbHeight * ratio);
                    width =  (int) (thumbWidth * ratio);
                } else {
                    height = targetHeight;
                    width = targetWidth;
                }
                decodeBitmapCompleted(receiver,
                        Bitmap.createScaledBitmap(drawableThumb.getBitmap(), width, height, true));
                return;
            } else if (thumb != null) {
                Bitmap bitmap;
                if (thumb.getIntrinsicWidth() > 0 && thumb.getIntrinsicHeight() > 0) {
                    bitmap = Bitmap.createBitmap(thumb.getIntrinsicWidth(),
                            thumb.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                } else {
                    decodeBitmapCompleted(receiver, null);
                    return;
                }

                Canvas canvas = new Canvas(bitmap);
                thumb.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                thumb.draw(canvas);
                decodeBitmapCompleted(receiver,
                        Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true));
                return;
            }
            decodeBitmapCompleted(receiver, null);
        });
    }

    @Override
    public void decodeBitmap(BitmapReceiver receiver) {
        sExecutorService.execute(() -> {
            Drawable thumb = getThumbnailDrawable(ORIGINAL_SIZE, new CancellationSignal());
            Bitmap bitmap = null;
            // Live wallpaper components may or may not specify a thumbnail drawable.
            if (thumb instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) thumb).getBitmap();
            } else if (thumb != null) {
                if (thumb.getIntrinsicWidth() > 0 && thumb.getIntrinsicHeight() > 0) {
                    bitmap = Bitmap.createBitmap(thumb.getIntrinsicWidth(),
                            thumb.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                }
            }
            decodeBitmapCompleted(receiver, bitmap);
        });
    }

    @Override
    public void decodeBitmapRegion(Rect rect, int targetWidth, int targetHeight,
            boolean shouldAdjustForRtl, BitmapReceiver receiver) {
        receiver.onBitmapDecoded(null);
    }

    @Override
    public void decodeRawDimensions(Activity unused, DimensionsReceiver receiver) {
        // TODO(b/277166654): Reuse the logic for all thumb asset decoding
        sExecutorService.execute(() -> {
            Bitmap result = null;
            Drawable thumb = mInfo.loadThumbnail(mContext.getPackageManager());
            if (thumb instanceof BitmapDrawable) {
                result = ((BitmapDrawable) thumb).getBitmap();
            } else if (thumb instanceof LayerDrawable) {
                Drawable layer = mLayerResolver.resolveLayer((LayerDrawable) thumb);
                if (layer instanceof BitmapDrawable) {
                    result = ((BitmapDrawable) layer).getBitmap();
                }
            }
            final Bitmap lr = result;
            new Handler(Looper.getMainLooper()).post(
                    () ->
                            receiver.onDimensionsDecoded(
                                    lr == null ? null : new Point(lr.getWidth(), lr.getHeight()))
            );
        });
    }

    @Override
    public boolean supportsTiling() {
        return false;
    }

    @Override
    public void loadDrawable(Context context, ImageView imageView,
                             int placeholderColor) {
        imageView.setBackgroundColor(placeholderColor);
        Glide.with(context)
                .asDrawable()
                .load(LiveWallpaperThumbAsset.this)
                .apply(RequestOptions.centerCropTransform()
                        .placeholder(new ColorDrawable(placeholderColor))
                        .apply(cacheOptions()))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }

    @Override
    public void loadDrawableWithTransition(Context context, ImageView imageView,
            final int transitionDurationMillis,
            @Nullable final DrawableLoadedListener drawableLoadedListener,
            int placeholderColor, @Nullable PermissionErrorListener permissionErrorListener) {
        imageView.setBackgroundColor(placeholderColor);
        Glide.with(context)
                .asDrawable()
                .load(LiveWallpaperThumbAsset.this)
                .apply(RequestOptions.centerCropTransform()
                        .placeholder(new ColorDrawable(placeholderColor))
                        .apply(cacheOptions()))
                .transition(DrawableTransitionOptions.withCrossFade(transitionDurationMillis))
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            Target<Drawable> target, boolean isFirstResource) {
                        // Glide shows the placeholder for a thumbnail that fails to load, and the
                        // placeholder is then the final image of the view.
                        if (drawableLoadedListener != null) {
                            drawableLoadedListener.onDrawableLoaded();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource dataSource,
                            boolean isFirstResource) {
                        if (drawableLoadedListener != null) {
                            drawableLoadedListener.onDrawableLoaded();
                        }
                        return false;
                    }
                })
                .into(imageView);
    }

    @Override
    public void loadLowResDrawable(Activity activity, ImageView imageView, int placeholderColor,
            BitmapTransformation transformation) {
        Transformation<Bitmap> finalTransformation = (transformation == null)
                ? new FitCenter()
                : new MultiTransformation<>(new FitCenter(), transformation);
        Glide.with(activity)
                .asDrawable()
                .load(LiveWallpaperThumbAsset.this)
                .apply(RequestOptions.bitmapTransform(finalTransformation)
                        .placeholder(new ColorDrawable(placeholderColor))
                        .apply(cacheOptions()))
                .into(imageView);
    }

    @Override
    @WorkerThread
    public Bitmap getLowResBitmap(Context context) {
        try {
            Drawable drawable = Glide.with(context)
                    .asDrawable()
                    .load(this)
                    .apply(cacheOptions())
                    .submit()
                    .get(LOW_RES_THUMB_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null) {
                    return bitmap;
                }
            }
            Bitmap bitmap;
            // If not a bitmap, draw the drawable into a bitmap
            if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                return null;
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(), Bitmap.Config.RGB_565);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.w(TAG, "Couldn't obtain low res bitmap", e);
        }
        return null;
    }

    /**
     * Returns the cache options of every Glide load of this asset. An image at a URI can change,
     * for example with the theme colors, so Glide skips its memory and disk caches for it.
     */
    private RequestOptions cacheOptions() {
        return mUri != null
                ? RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE).skipMemoryCache(true)
                : new RequestOptions();
    }

    /**
     * Returns a Glide cache key.
     */
    Key getKey() {
        return new LiveWallpaperThumbKey(mInfo);
    }

    /**
     * Returns the thumbnail drawable for the live wallpaper synchronously. Should not be called on
     * the main UI thread.
     *
     * <p>Caches the thumbnail of an asset without a URI. An image at a URI can change, so it is
     * read again on each call.
     *
     * @param size the size in pixels at which the thumbnail is shown, see
     *             {@link ThumbnailImage#readThumbnailImage}
     * @param signal cancels reading the image at the URI
     * @throws android.os.OperationCanceledException when {@code signal} is cancelled
     */
    @WorkerThread
    protected Drawable getThumbnailDrawable(Size size, CancellationSignal signal) {
        if (mUri == null) {
            if (mCachedThumbnail == null) mCachedThumbnail = loadThumbnailFromInfo();
            return mCachedThumbnail;
        }
        Drawable fromUri = loadThumbnailFromUri(size, signal);
        return (fromUri != null) ? fromUri : loadThumbnailFromInfo();
    }

    @Nullable
    private Drawable loadThumbnailFromUri(Size size, CancellationSignal signal) {
        try {
            return new BitmapDrawable(mContext.getResources(), ThumbnailImage.readThumbnailImage(
                    mContext.getContentResolver(), mUri, size,
                    InjectorProvider.getInjector().getDisplayUtils(mContext)
                            .getMaxDisplaysDimension(),
                    signal));
        } catch (IOException e) {
            Log.w(TAG, "The thumbnail at " + mUri + " could not be read.", e);
        } catch (SecurityException e) {
            Log.w(TAG, "The picker is not allowed to read the thumbnail at " + mUri + ".", e);
        }
        return null;
    }

    private Drawable loadThumbnailFromInfo() {
        return mInfo.loadThumbnail(mContext.getPackageManager());
    }

    /**
     * Glide caching key for resources from any arbitrary package.
     */
    private static final class LiveWallpaperThumbKey implements Key {
        private android.app.WallpaperInfo mInfo;

        LiveWallpaperThumbKey(android.app.WallpaperInfo info) {
            mInfo = info;
        }

        @Override
        public String toString() {
            return getCacheKey();
        }

        @Override
        public int hashCode() {
            return getCacheKey().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof LiveWallpaperThumbKey)) {
                return false;
            }

            LiveWallpaperThumbKey otherKey = (LiveWallpaperThumbKey) object;
            return getCacheKey().equals(otherKey.getCacheKey());
        }

        @Override
        public void updateDiskCacheKey(MessageDigest messageDigest) {
            messageDigest.update(getCacheKey().getBytes(CHARSET));
        }

        /**
         * Returns an inexpensively calculated {@link String} suitable for use as a disk cache key,
         * based on the live wallpaper's package name and service name, which is enough to uniquely
         * identify a live wallpaper.
         */
        private String getCacheKey() {
            return "LiveWallpaperThumbKey{"
                    + "packageName=" + mInfo.getPackageName() + ","
                    + "serviceName=" + mInfo.getServiceName()
                    + '}';
        }
    }
}
