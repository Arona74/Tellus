package com.yucareux.tellus.worldgen;

import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

/**
 * Geometry policy for the protective terrain capsule placed around buried
 * structures that extend below Tellus's configured terrain shell.
 */
final class UndergroundStructureProtection {
   static final int BEDROCK_THICKNESS = 1;
   static final int STONE_THICKNESS = 4;
   static final int TOTAL_THICKNESS = BEDROCK_THICKNESS + STONE_THICKNESS;

   private UndergroundStructureProtection() {
   }

   /**
    * Beard-box structures can span hundreds of blocks while their actual pieces
    * occupy an irregular footprint. Protecting the start's aggregate box creates
    * a huge artificial cuboid, so these structures use piece-shaped envelopes.
    */
   static boolean usesPieceScopedTerrainEnvelope(TerrainAdjustment adjustment) {
      return adjustment == TerrainAdjustment.BEARD_BOX;
   }

   static int minimumStructureY(int worldMinY) {
      return worldMinY + TOTAL_THICKNESS;
   }

   static int protectionBottomY(int structureMinY) {
      return structureMinY - TOTAL_THICKNESS;
   }

   static int terrainShellBottomY(int surfaceY, int undergroundDepth) {
      return surfaceY - Math.max(0, undergroundDepth);
   }

   static boolean needsTerrainExtension(int structureMinY, int surfaceY, int undergroundDepth) {
      return protectionBottomY(structureMinY) < terrainShellBottomY(surfaceY, undergroundDepth);
   }

   static int protectionFillTopY(
      int terrainShellBottomY, int structureMinY, boolean pieceScoped, boolean preserveStructureCores
   ) {
      return pieceScoped && preserveStructureCores
         ? Math.min(terrainShellBottomY, structureMinY - 1)
         : terrainShellBottomY;
   }

   /**
    * The capsule has an outer bedrock floor and side wall. Its top remains open
    * so the extended volume joins the normal terrain shell and cave network.
    */
   static boolean isOuterBedrockSkin(
      int x, int y, int z, int expandedMinX, int protectionBottomY, int expandedMinZ, int expandedMaxX, int expandedMaxZ
   ) {
      return isOuterBedrockSkin(
         x,
         y,
         z,
         expandedMinX,
         protectionBottomY,
         expandedMinZ,
         expandedMaxX,
         expandedMaxZ,
         true
      );
   }

   static boolean isOuterBedrockSkin(
      int x,
      int y,
      int z,
      int expandedMinX,
      int protectionBottomY,
      int expandedMinZ,
      int expandedMaxX,
      int expandedMaxZ,
      boolean includeSideWalls
   ) {
      return y == protectionBottomY
         || includeSideWalls
            && (x == expandedMinX || x == expandedMaxX || z == expandedMinZ || z == expandedMaxZ);
   }
}
