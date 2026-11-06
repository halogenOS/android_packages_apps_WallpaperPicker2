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

import android.graphics.drawable.Drawable;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.util.Size;

import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

/**
 * Glide model loader for live wallpaper thumbnails.
 */
public class LiveWallpaperThumbAssetLoader implements
        ModelLoader<LiveWallpaperThumbAsset, Drawable> {

    @Override
    public boolean handles(LiveWallpaperThumbAsset liveWallpaperThumbAsset) {
        return true;
    }

    @Nullable
    @Override
    public LoadData<Drawable> buildLoadData(LiveWallpaperThumbAsset liveWallpaperThumbAsset,
                                            int width, int height, Options options) {
        return new LoadData<>(liveWallpaperThumbAsset.getKey(),
                new LiveWallpaperThumbFetcher(liveWallpaperThumbAsset, new Size(width, height)));
    }

    /**
     * Factory that constructs {@link LiveWallpaperThumbAssetLoader} instances.
     */
    public static class LiveWallpaperThumbAssetLoaderFactory
            implements ModelLoaderFactory<LiveWallpaperThumbAsset, Drawable> {
        public LiveWallpaperThumbAssetLoaderFactory() {
        }

        @Override
        public ModelLoader<LiveWallpaperThumbAsset, Drawable> build(
                MultiModelLoaderFactory multiFactory) {
            return new LiveWallpaperThumbAssetLoader();
        }

        @Override
        public void teardown() {
            // no-op
        }
    }

    /**
     * Fetcher class for fetching wallpaper image data from a {@link LiveWallpaperThumbAsset}.
     */
    private static class LiveWallpaperThumbFetcher implements DataFetcher<Drawable> {

        private final LiveWallpaperThumbAsset mLiveWallpaperThumbAsset;
        private final Size mSize;
        private final CancellationSignal mCancellationSignal = new CancellationSignal();

        LiveWallpaperThumbFetcher(LiveWallpaperThumbAsset liveWallpaperThumbAsset, Size size) {
            mLiveWallpaperThumbAsset = liveWallpaperThumbAsset;
            mSize = size;
        }

        @Override
        public void loadData(Priority priority, DataCallback<? super Drawable> callback) {
            try {
                callback.onDataReady(
                        mLiveWallpaperThumbAsset.getThumbnailDrawable(mSize, mCancellationSignal));
            } catch (OperationCanceledException e) {
                callback.onLoadFailed(e);
            }
        }

        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }

        @Override
        public void cancel() {
            mCancellationSignal.cancel();
        }

        @Override
        public void cleanup() {
            // no-op
        }

        @Override
        public Class<Drawable> getDataClass() {
            return Drawable.class;
        }
    }
}
