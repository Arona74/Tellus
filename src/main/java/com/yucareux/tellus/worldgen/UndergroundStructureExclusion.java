package com.yucareux.tellus.worldgen;

import java.util.List;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

/**
 * Piece-level exclusion geometry used to keep terrain-adapted underground
 * structures separate from projected caves and cave-biome decoration.
 */
public final class UndergroundStructureExclusion {
   public static final int MINIMUM_TERRAIN_COVER = 8;
   public static final int CARVER_MARGIN = 4;
   public static final int FEATURE_PLACEMENT_MARGIN = 16;

   private UndergroundStructureExclusion() {
   }

   /**
    * Beard-box structures already rely on piece-aware terrain shaping. Treating
    * every piece as a hard cave and feature exclusion removes the cavern and
    * nearly all ore from an ancient city's footprint.
    */
   public static boolean protectsProjectedGeneration(TerrainAdjustment adjustment) {
      return adjustment != TerrainAdjustment.NONE && adjustment != TerrainAdjustment.BEARD_BOX;
   }

   public static boolean isUnderground(int pieceMaxY, int terrainSurfaceY) {
      return pieceMaxY <= terrainSurfaceY - MINIMUM_TERRAIN_COVER;
   }

   public static boolean blocksCarving(List<Box> boxes, int x, int y, int z) {
      return containsExpanded(boxes, x, y, z, CARVER_MARGIN);
   }

   public static boolean blocksFeaturePlacement(List<Box> boxes, int x, int y, int z) {
      return containsExpanded(boxes, x, y, z, FEATURE_PLACEMENT_MARGIN);
   }

   private static boolean containsExpanded(List<Box> boxes, int x, int y, int z, int margin) {
      if (boxes == null || boxes.isEmpty()) {
         return false;
      }

      for (Box box : boxes) {
         if (box.containsExpanded(x, y, z, margin)) {
            return true;
         }
      }
      return false;
   }

   public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      public Box {
         if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Underground structure exclusion bounds are inverted");
         }
      }

      public boolean containsExpanded(int x, int y, int z, int margin) {
         int safeMargin = Math.max(0, margin);
         return (long)x >= (long)this.minX - safeMargin
            && (long)x <= (long)this.maxX + safeMargin
            && (long)y >= (long)this.minY - safeMargin
            && (long)y <= (long)this.maxY + safeMargin
            && (long)z >= (long)this.minZ - safeMargin
            && (long)z <= (long)this.maxZ + safeMargin;
      }

      public boolean intersectsHorizontal(int minX, int minZ, int maxX, int maxZ, int margin) {
         int safeMargin = Math.max(0, margin);
         return (long)this.maxX + safeMargin >= minX
            && (long)this.minX - safeMargin <= maxX
            && (long)this.maxZ + safeMargin >= minZ
            && (long)this.minZ - safeMargin <= maxZ;
      }
   }
}
