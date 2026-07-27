package com.yucareux.tellus.world.data.elevation;

import java.util.Arrays;

/**
 * Elevation raster.
 *
 * <p>Historically backed by {@code short} (whole-metre) samples; now backed by {@code float}
 * so the sub-metre precision decoded from Terrarium tiles can be preserved when the
 * sub-metre-elevation option is enabled. The class name is retained to avoid a wide rename.
 * {@link #get} returns a {@code double} and {@link #set} accepts a {@code double}; when the
 * option is off, callers store whole-metre (rounded) values, so behaviour is unchanged.
 */
final class ShortRaster {
   private final int width;
   private final int height;
   private final float[] data;

   private ShortRaster(int width, int height, float[] data) {
      this.width = width;
      this.height = height;
      this.data = data;
   }

   static ShortRaster create(int width, int height) {
      return new ShortRaster(width, height, new float[checkedSampleCount(width, height)]);
   }

   static ShortRaster wrap(int width, int height, float[] data) {
      if (data.length != checkedSampleCount(width, height)) {
         throw new IllegalArgumentException("Invalid raster buffer");
      } else {
         return new ShortRaster(width, height, data);
      }
   }

   private static int checkedSampleCount(int width, int height) {
      if (width <= 0 || height <= 0) {
         throw new IllegalArgumentException("Raster dimensions must be positive");
      }
      try {
         return Math.multiplyExact(width, height);
      } catch (ArithmeticException error) {
         throw new IllegalArgumentException("Raster dimensions are too large", error);
      }
   }

   int width() {
      return this.width;
   }

   int height() {
      return this.height;
   }

   double get(int x, int y) {
      return this.data[x + y * this.width];
   }

   void set(int x, int y, double value) {
      this.data[x + y * this.width] = (float)value;
   }

   void fill(double value) {
      Arrays.fill(this.data, (float)value);
   }
}
