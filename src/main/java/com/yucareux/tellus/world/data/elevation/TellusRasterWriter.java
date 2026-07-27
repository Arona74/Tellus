package com.yucareux.tellus.world.data.elevation;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

final class TellusRasterWriter {
   private static final byte[] SIGNATURE = "TELLUS/RASTER".getBytes(StandardCharsets.US_ASCII);
   private static final int FORMAT_SHORT = 2;
   private static final int FILTER_LEFT = 1;

   private TellusRasterWriter() {
   }

   static void writeShortRaster(OutputStream output, ShortRaster raster) throws IOException {
      try (DataOutputStream dataOut = new DataOutputStream(output)) {
         dataOut.write(SIGNATURE);
         dataOut.writeByte(0);
         dataOut.writeInt(raster.width());
         dataOut.writeInt(raster.height());
         dataOut.writeByte(FORMAT_SHORT);
         writeChunk(dataOut, raster);
      }
   }

   private static void writeChunk(DataOutputStream output, ShortRaster raster) throws IOException {
      ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
      try (DataOutputStream chunkOut = new DataOutputStream(chunkBuffer)) {
         chunkOut.writeInt(0);
         chunkOut.writeInt(0);
         chunkOut.writeInt(raster.width());
         chunkOut.writeInt(raster.height());
         chunkOut.writeByte(FILTER_LEFT);
         LZMA2Options options = new LZMA2Options();
         options.setPreset(3);
         try (XZOutputStream xzOut = new XZOutputStream(chunkOut, options); DataOutputStream xzData = new DataOutputStream(xzOut)) {
            writeLeftFiltered(raster, xzData);
         }
      }

      byte[] chunkBytes = chunkBuffer.toByteArray();
      output.writeInt(chunkBytes.length);
      output.write(chunkBytes);
   }

   private static void writeLeftFiltered(ShortRaster raster, DataOutputStream output) throws IOException {
      // The on-disk cache format stores whole-metre shorts. The in-memory raster is now float
      // (to preserve sub-metre precision during a generation session); it is rounded here when
      // written to disk. Sub-metre precision therefore does not survive a disk-cache round-trip
      // (a follow-up could add a float cache format), but it is available for freshly decoded /
      // memory-cached tiles, which is what terrain generation uses.
      for (int y = 0; y < raster.height(); y++) {
         int previous = 0;
         for (int x = 0; x < raster.width(); x++) {
            int current = (int)Math.round(raster.get(x, y));
            output.writeShort((short)(current - previous));
            previous = current;
         }
      }
   }
}
