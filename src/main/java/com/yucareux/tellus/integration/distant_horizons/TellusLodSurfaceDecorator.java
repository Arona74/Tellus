package com.yucareux.tellus.integration.distant_horizons;

import java.util.function.DoubleSupplier;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Lets another mod cover the bare ground of Tellus's direct Distant Horizons LOD columns, the
 * columns {@link TellusLodGenerator} builds from elevation data without generating chunks (so
 * nothing that decorates chunks ever sees them).
 *
 * <p>Called once per LOD column, on DH's world generator threads, only when the column's topmost
 * terrain block sits at {@code surfaceY} and the block above it is air (so never under water,
 * buildings, bridges or vegetation drawn over the ground). The returned state is put in that one
 * block above the surface; {@code null} leaves the column as it is.
 *
 * <p>Register with {@link TellusLodHooks#registerSurfaceDecorator}. Only types from Minecraft and
 * the JDK appear here, so a mod can implement it through reflection (a {@link java.lang.reflect.Proxy})
 * without depending on Tellus or Distant Horizons.
 */
@FunctionalInterface
public interface TellusLodSurfaceDecorator {
   /**
    * @param generator the level's Tellus chunk generator
    * @param worldX block X of the column (for a coarse LOD, the cell it represents)
    * @param worldZ block Z of the column
    * @param surfaceY absolute Y of the column's top terrain block
    * @param surfaceBlock that block
    * @param biome the column's biome
    * @param sampleResolutionMeters the elevation resolution this LOD was sampled at
    * @param continuousSurface the column's surface height before rounding to whole blocks
    *        ({@link com.yucareux.tellus.worldgen.EarthChunkGenerator#sampleLodContinuousSurface}),
    *        NaN when the elevation is not in memory; computed on demand
    * @return the block to place above the surface, or {@code null} for none
    */
   BlockState surfaceCover(
      ChunkGenerator generator,
      int worldX,
      int worldZ,
      int surfaceY,
      BlockState surfaceBlock,
      Holder<Biome> biome,
      double sampleResolutionMeters,
      DoubleSupplier continuousSurface
   );
}
