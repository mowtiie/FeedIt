package com.mowtiie.feedit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class)
public class ImageHolderTest {

    private static final class SizedDrawable extends Drawable {
        private final int width;
        private final int height;

        SizedDrawable(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return width;
        }

        @Override
        public int getIntrinsicHeight() {
            return height;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
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

    @Test
    public void wideImageIsScaledDownKeepingAspectRatio() {
        GlideImageGetter.ImageHolder holder = new GlideImageGetter.ImageHolder();

        assertTrue(holder.setImage(new SizedDrawable(1000, 500), 400));

        assertEquals(400, holder.getBounds().width());
        assertEquals(200, holder.getBounds().height());
    }

    @Test
    public void smallImageKeepsItsSize() {
        GlideImageGetter.ImageHolder holder = new GlideImageGetter.ImageHolder();

        assertTrue(holder.setImage(new SizedDrawable(120, 80), 400));

        assertEquals(120, holder.getBounds().width());
        assertEquals(80, holder.getBounds().height());
    }

    @Test
    public void trackingPixelsAreIgnored() {
        GlideImageGetter.ImageHolder holder = new GlideImageGetter.ImageHolder();

        assertFalse(holder.setImage(new SizedDrawable(1, 1), 400));

        assertTrue(holder.getBounds().isEmpty());
    }
}
