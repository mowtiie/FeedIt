package com.mowtiie.feedit.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads inline article images with Glide. Images start as empty placeholders and are filled in as
 * they arrive, after which {@code onImageLoaded} runs on the main thread so the text can re-layout.
 */
public class GlideImageGetter implements Html.ImageGetter {

    private static final int MIN_IMAGE_SIZE_PX = 16;

    private final Context appContext;
    private final int maxWidthPx;
    private final String skipUrl;
    private final Runnable onImageLoaded;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<CustomTarget<Drawable>> targets = new ArrayList<>();
    private boolean cancelled;

    public GlideImageGetter(Context context, int maxWidthPx, @Nullable String skipUrl, Runnable onImageLoaded) {
        this.appContext = context.getApplicationContext();
        this.maxWidthPx = maxWidthPx;
        this.skipUrl = skipUrl;
        this.onImageLoaded = onImageLoaded;
    }

    @Override
    public Drawable getDrawable(String source) {
        ImageHolder holder = new ImageHolder();
        if (!isLoadable(source)) {
            return holder;
        }
        mainHandler.post(() -> {
            if (cancelled) {
                return;
            }
            CustomTarget<Drawable> target = new CustomTarget<Drawable>(maxWidthPx, maxWidthPx) {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    if (holder.setImage(resource, maxWidthPx)) {
                        onImageLoaded.run();
                    }
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                }
            };
            targets.add(target);
            Glide.with(appContext).load(source).into(target);
        });
        return holder;
    }

    /** Must be called on the main thread. */
    public void cancelAll() {
        cancelled = true;
        for (CustomTarget<Drawable> target : targets) {
            Glide.with(appContext).clear(target);
        }
        targets.clear();
    }

    private boolean isLoadable(String source) {
        if (source == null || source.equals(skipUrl)) {
            return false;
        }
        String lower = source.toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    static final class ImageHolder extends Drawable {

        private Drawable image;

        /** Scales the image to fit {@code maxWidth}; returns false when it is too small to show. */
        boolean setImage(Drawable drawable, int maxWidth) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width < MIN_IMAGE_SIZE_PX || height < MIN_IMAGE_SIZE_PX) {
                return false;
            }
            if (width > maxWidth) {
                height = height * maxWidth / width;
                width = maxWidth;
            }
            drawable.setBounds(0, 0, width, height);
            image = drawable;
            setBounds(0, 0, width, height);
            invalidateSelf();
            return true;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (image != null) {
                image.draw(canvas);
            }
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
