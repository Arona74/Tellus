/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.core.dataObjects.render.textures.BlockFaceTexture;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.coreapi.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

#if MC_VER <= MC_1_12_2
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
#else
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
#endif

#if MC_VER >= MC_1_19_2
import net.minecraft.util.RandomSource;
#else
#endif

#if MC_VER < MC_1_21_5
import net.minecraft.client.renderer.block.model.BakedQuad;
#elif MC_VER <= MC_1_21_11
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BakedQuad;
#else
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3fc;

import javax.imageio.ImageIO;
#endif

/**
 * Bakes and stores a small texture for each face of a block state
 * by software rasterizing the state's model quads. <br>
 * This allows LODs to show the block's actual texture and silhouette
 * (IE fence posts and flower cutouts) instead of a single flat color. <br><br>
 *
 * Mod blocks are handled the same way vanilla blocks are since their
 * baked models and sprites are rasterized directly,
 * blocks without usable models (IE blocks rendered via block entities)
 * fall back to their particle texture.
 *
 * @see ClientBlockStateColorCache
 * @see BlockFaceTexture
 */
public class ClientBlockStateTextureCache
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	#if MC_VER <= MC_1_12_2
	private static final Minecraft MC = Minecraft.getMinecraft();
	#else
	private static final Minecraft MC = Minecraft.getInstance();
	#endif
	
	/**
	 * The resolution face textures are baked at. <br>
	 * Sprites with a higher resolution (IE from resource packs) are down-sampled.
	 */
	public static final int TEXTURE_WIDTH_AND_HEIGHT = 16;
	
	/** The bake order */
	private static final EDhDirection[] FACE_DIRECTIONS =
		{
			EDhDirection.DOWN,
			EDhDirection.UP,
			EDhDirection.NORTH,
			EDhDirection.SOUTH,
			EDhDirection.WEST,
			EDhDirection.EAST
		};
	
	/**
	 * Methods using MC's "RandomSource" object aren't thread safe <br>
	 * so we need to put locks around that logic. <br>
	 * specifically:
	 * <code>
	 * getBlockModel(blockState).getQuads(blockState, direction, RANDOM)
	 * </code>
	 */
	private static final ReentrantLock BAKE_LOCK = new ReentrantLock();
	
	#if MC_VER < MC_1_19_2
	private static final Random RANDOM = new Random(0);
	#else
	/** Note: this object isn't thread safe and must be put in a lock */
	private static final RandomSource RANDOM = RandomSource.create();
	#endif
	
	#if MC_VER <= MC_1_12_2
	private static final ConcurrentHashMap<IBlockState, BlockFaceTexture[]> TEXTURES_BY_BLOCK_STATE = new ConcurrentHashMap<>();
	#else
	private static final ConcurrentHashMap<BlockState, BlockFaceTexture[]> TEXTURES_BY_BLOCK_STATE = new ConcurrentHashMap<>();
	#endif
	
	/** 
	 * can be enabled to make sure the textures are being parsed
	 * with the correct colors.
	 */
	private static final boolean WRITE_TEXTURES_TO_FILE_FOR_DEBUGGING = false;
	/** should end with a "/" */
	private static final String TEST_TEXTURE_OUTPUT_FOLDER_PATH = "C:/Users/James_Seibel/Desktop/tex_output/";
	
	
	
	//================//
	// public getters //
	//================//
	//region
	
	public static BlockFaceTexture getFaceTexture(BlockStateWrapper blockStateWrapper, EDhDirection direction)
	{
		BlockFaceTexture[] faceTextures = TEXTURES_BY_BLOCK_STATE.computeIfAbsent(
				blockStateWrapper.blockState,
				(blockState) ->
				{
					BlockFaceTexture[] blockFaceTextures = bakeAllFaceTextures(blockStateWrapper);
					if (WRITE_TEXTURES_TO_FILE_FOR_DEBUGGING)
					{
						writeTopAndNorthTexturesToFile(blockStateWrapper, blockFaceTextures);
					}
					return blockFaceTextures;
				});
		return faceTextures[direction.faceIndex];
	}
	
	/** Should be called whenever MC's textures change, IE when resource packs are swapped. */
	public static void clearCache() { TEXTURES_BY_BLOCK_STATE.clear(); }
	
	//endregion
	
	
	
	//========//
	// baking //
	//========//
	//region
	
	private static BlockFaceTexture[] bakeAllFaceTextures(BlockStateWrapper blockStateWrapper)
	{
		BlockFaceTexture[] faceTextures = new BlockFaceTexture[FACE_DIRECTIONS.length];
		
		try
		{
			// getQuads() and RANDOM aren't thread safe so all baking is done in a lock
			BAKE_LOCK.lock();
			
			if (BlockStateWrapper.isAir(blockStateWrapper.blockState))
			{
				// Air shouldn't be rendered and is just here to capture the rare bug state where air's texture is requested.
				Arrays.fill(faceTextures, BlockFaceTexture.createSolidColor(ColorUtil.INVISIBLE, false));
				return faceTextures;
			}
			
			if (blockStateWrapper.isLiquid())
			{
				// TODO: Works fine for water but not lava
				// liquids don't have models to rasterize, bake their sprite directly,
				// tinting is needed for biome dependent water colors
				BlockFaceTexture liquidTexture = bakeSpriteTexture(getParticleSprite(blockStateWrapper), true);
				Arrays.fill(faceTextures, liquidTexture);
				return faceTextures;
			}
			
			for (int faceIndex = 0; faceIndex < FACE_DIRECTIONS.length; faceIndex++)
			{
				faceTextures[faceIndex] = bakeFaceTexture(blockStateWrapper, FACE_DIRECTIONS[faceIndex]);
			}
		}
		catch (Exception bakeError)
		{
			LOGGER.warn("Failed to bake face textures for block ["+blockStateWrapper.getSerialString()+"], error: ["+bakeError.getMessage()+"], block will render as hot pink.", bakeError);
		}
		finally
		{
			BAKE_LOCK.unlock();
		}
		
		for (int faceIndex = 0; faceIndex < faceTextures.length; faceIndex++)
		{
			if (faceTextures[faceIndex] == null)
			{
				faceTextures[faceIndex] = BlockFaceTexture.createSolidColor(ColorUtil.HOT_PINK, false);
			}
		}
		
		
		
		return faceTextures;
	}
	
	private static BlockFaceTexture bakeFaceTexture(BlockStateWrapper blockStateWrapper, EDhDirection dhDirection)
	{
		//==============//
		// quad lookup  //
		//==============//
		
		ArrayList<BakedQuad> quadList = new ArrayList<>();
		try
		{
			List<BakedQuad> directionQuads = getQuads(blockStateWrapper.blockState, dhDirection);
			if (directionQuads != null && !directionQuads.isEmpty())
			{
				BakedQuad faceQuad = pickFaceQuad(directionQuads);
				TextureAtlasSprite quadSprite = getQuadSprite(faceQuad);
				boolean isQuadTinted = isQuadTinted(faceQuad);	
				
				// Faces with culled quads cover the whole face (IE full cubes),
				// copying the quad's sprite directly is both more reliable
				// and more accurate than rasterizing.
				BlockFaceTexture texture = bakeSpriteTexture(quadSprite, isQuadTinted);
				return texture; 
			}
			
			// unculled quads (IE fence posts)
			// can be visible from every direction
			List<BakedQuad> unculledQuads = getQuads(blockStateWrapper.blockState, null);
			if (unculledQuads != null)
			{
				quadList.addAll(unculledQuads);
			}
		}
		catch (Exception ignore)
		{
			// failing to get quads can happen if the block is invalid
			// (i.e. AIR is somehow passed in)
		}
		
		if (quadList.isEmpty())
		{
			// blocks without quads (IE blocks rendered via block entities)
			// fall back to their particle texture
			return bakeSpriteTexture(getParticleSprite(blockStateWrapper), false);
		}
		
		
		
		//===============//
		// quad decoding //
		//===============//
		
		ArrayList<QuadGeometry> geometryList = new ArrayList<>(quadList.size());
		boolean anyQuadTinted = false;
		boolean anyQuadUntinted = false;
		for (int quadIndex = 0; quadIndex < quadList.size(); quadIndex++)
		{
			QuadGeometry geometry = decodeQuad(quadList.get(quadIndex), dhDirection);
			geometryList.add(geometry);
			
			anyQuadTinted |= geometry.tinted;
			anyQuadUntinted |= !geometry.tinted;
		}
		
		// Tinting can only be applied to the texture as a whole,
		// so when a face mixes tinted and untinted quads (IE grass block sides
		// where a tinted grass overlay covers untinted dirt) the tinted quads are skipped,
		// otherwise the untinted quads' colors would be tinted as well.
		boolean skipTintedQuads = anyQuadTinted && anyQuadUntinted;
		boolean textureTinted = anyQuadTinted && !anyQuadUntinted;
		
		// quads are drawn back to front so quads closer
		// to the viewed face overwrite the ones behind them
		geometryList.sort(Comparator.comparingDouble(QuadGeometry::getAverageDepth));
		
		
		
		//===============//
		// rasterization //
		//===============//
		
		int[] pixels = new int[TEXTURE_WIDTH_AND_HEIGHT * TEXTURE_WIDTH_AND_HEIGHT];
		boolean anyPixelDrawn = false;
		for (int geometryIndex = 0; geometryIndex < geometryList.size(); geometryIndex++)
		{
			QuadGeometry geometry = geometryList.get(geometryIndex);
			if (skipTintedQuads && geometry.tinted)
			{
				continue;
			}
			
			anyPixelDrawn |= rasterizeQuad(geometry, pixels);
		}
		
		if (!anyPixelDrawn)
		{
			// nothing of this block is visible from this direction,
			// fall back to the particle texture since LODs expect
			// every face of a non-air block to be renderable
			return bakeSpriteTexture(getParticleSprite(blockStateWrapper), false);
		}
		
		return new BlockFaceTexture(TEXTURE_WIDTH_AND_HEIGHT, TEXTURE_WIDTH_AND_HEIGHT, pixels, textureTinted);
	}
	
	/**
	 * Picks which quad represents the face when several overlap. <br>
	 * When tinted and untinted quads mix (IE grass block sides where a tinted
	 * grass overlay covers untinted dirt) the untinted quad is used since
	 * tinting can only be applied to the whole face texture,
	 * this matches how the flat color resolution handles those faces.
	 */
	private static BakedQuad pickFaceQuad(List<BakedQuad> quadList)
	{
		for (int i = 0; i < quadList.size(); i++)
		{
			if (!isQuadTinted(quadList.get(i)))
			{
				return quadList.get(i);
			}
		}
		return quadList.get(0);
	}
	
	private static TextureAtlasSprite getQuadSprite(BakedQuad quad)
	{
		#if MC_VER <= MC_1_12_2
		return quad.getSprite();
		#elif MC_VER < MC_1_17_1
		return quad.sprite;
		#elif MC_VER < MC_1_21_5
		return quad.getSprite();
		#elif MC_VER <= MC_1_21_11
		return quad.sprite();
		#else
		return quad.materialInfo().sprite();
		#endif
	}
	private static boolean isQuadTinted(BakedQuad quad)
	{
		#if MC_VER <= MC_1_12_2
		return quad.hasTintIndex();
		#elif MC_VER <= MC_1_21_11
		return quad.isTinted();
		#else
		return quad.materialInfo().isTinted();
		#endif
	}
	
	/** Copies the given sprite directly, used for blocks where rasterizing model quads isn't possible. */
	private static BlockFaceTexture bakeSpriteTexture(@Nullable TextureAtlasSprite sprite, boolean tinted)
	{
		if (sprite == null)
		{
			return BlockFaceTexture.createSolidColor(ColorUtil.HOT_PINK, false);
		}
		
		int spriteWidth = getSpriteWidth(sprite);
		int spriteHeight = getSpriteHeight(sprite);
		if (spriteWidth <= 0 || spriteHeight <= 0)
		{
			return BlockFaceTexture.createSolidColor(ColorUtil.HOT_PINK, false);
		}
		
		int[] pixels = new int[TEXTURE_WIDTH_AND_HEIGHT * TEXTURE_WIDTH_AND_HEIGHT];
		for (int v = 0; v < TEXTURE_WIDTH_AND_HEIGHT; v++)
		{
			for (int u = 0; u < TEXTURE_WIDTH_AND_HEIGHT; u++)
			{
				int texelX = (u * spriteWidth) / TEXTURE_WIDTH_AND_HEIGHT;
				int texelY = (v * spriteHeight) / TEXTURE_WIDTH_AND_HEIGHT;
				pixels[(v * TEXTURE_WIDTH_AND_HEIGHT) + u] = TextureAtlasSpriteWrapper.getPixelARGB(sprite, 0, texelX, texelY);
			}
		}
		return new BlockFaceTexture(TEXTURE_WIDTH_AND_HEIGHT, TEXTURE_WIDTH_AND_HEIGHT, pixels, tinted);
	}
	
	//endregion
	
	
	
	//===============//
	// rasterization //
	//===============//
	//region
	
	/**
	 * Draws the given quad into the pixel array
	 * by splitting it into two triangles and sampling
	 * the quad's sprite for each covered pixel.
	 *
	 * @return true if at least one pixel was drawn
	 */
	private static boolean rasterizeQuad(QuadGeometry geometry, int[] pixels)
	{
		boolean anyPixelDrawn = rasterizeTriangle(geometry, 0, 1, 2, pixels);
		anyPixelDrawn |= rasterizeTriangle(geometry, 0, 2, 3, pixels);
		return anyPixelDrawn;
	}
	
	/** @return true if at least one pixel was drawn */
	private static boolean rasterizeTriangle(QuadGeometry geometry, int vertexA, int vertexB, int vertexC, int[] pixels)
	{
		// face space positions, in the range [0,1] for anything on the face
		float faceAU = geometry.faceU[vertexA];
		float faceAV = geometry.faceV[vertexA];
		float faceBU = geometry.faceU[vertexB];
		float faceBV = geometry.faceV[vertexB];
		float faceCU = geometry.faceU[vertexC];
		float faceCV = geometry.faceV[vertexC];
		
		// twice the triangle's signed area,
		// also the denominator for barycentric weights
		float area = ((faceBU - faceAU) * (faceCV - faceAV)) - ((faceBV - faceAV) * (faceCU - faceAU));
		if (Math.abs(area) < 0.000001f)
		{
			// the triangle is degenerate or perpendicular to this face
			return false;
		}
		
		int spriteWidth = getSpriteWidth(geometry.sprite);
		int spriteHeight = getSpriteHeight(geometry.sprite);
		if (spriteWidth <= 0 || spriteHeight <= 0)
		{
			return false;
		}
		
		// only check pixels inside the triangle's bounding box
		int minPixelU = Math.max((int) Math.floor(Math.min(faceAU, Math.min(faceBU, faceCU)) * TEXTURE_WIDTH_AND_HEIGHT), 0);
		int maxPixelU = Math.min((int) Math.ceil(Math.max(faceAU, Math.max(faceBU, faceCU)) * TEXTURE_WIDTH_AND_HEIGHT), TEXTURE_WIDTH_AND_HEIGHT - 1);
		int minPixelV = Math.max((int) Math.floor(Math.min(faceAV, Math.min(faceBV, faceCV)) * TEXTURE_WIDTH_AND_HEIGHT), 0);
		int maxPixelV = Math.min((int) Math.ceil(Math.max(faceAV, Math.max(faceBV, faceCV)) * TEXTURE_WIDTH_AND_HEIGHT), TEXTURE_WIDTH_AND_HEIGHT - 1);
		
		boolean anyPixelDrawn = false;
		for (int pixelV = minPixelV; pixelV <= maxPixelV; pixelV++)
		{
			for (int pixelU = minPixelU; pixelU <= maxPixelU; pixelU++)
			{
				// sample at the pixel's center
				float sampleU = (pixelU + 0.5f) / TEXTURE_WIDTH_AND_HEIGHT;
				float sampleV = (pixelV + 0.5f) / TEXTURE_WIDTH_AND_HEIGHT;
				
				// barycentric weights, all in [0,1] when the sample is inside the triangle
				float weightB = (((sampleU - faceAU) * (faceCV - faceAV)) - ((sampleV - faceAV) * (faceCU - faceAU))) / area;
				float weightC = (((faceBU - faceAU) * (sampleV - faceAV)) - ((faceBV - faceAV) * (sampleU - faceAU))) / area;
				float weightA = 1.0f - weightB - weightC;
				if (weightA < 0.0f || weightB < 0.0f || weightC < 0.0f)
				{
					continue;
				}
				
				// interpolate the sprite coordinates
				float spriteU = (weightA * geometry.spriteU[vertexA]) + (weightB * geometry.spriteU[vertexB]) + (weightC * geometry.spriteU[vertexC]);
				float spriteV = (weightA * geometry.spriteV[vertexA]) + (weightB * geometry.spriteV[vertexB]) + (weightC * geometry.spriteV[vertexC]);
				
				int texelX = Math.min(Math.max((int) (spriteU * spriteWidth), 0), spriteWidth - 1);
				int texelY = Math.min(Math.max((int) (spriteV * spriteHeight), 0), spriteHeight - 1);
				
				int argbSourceColor = TextureAtlasSpriteWrapper.getPixelARGB(geometry.sprite, 0, texelX, texelY);
				if (ColorUtil.getAlpha(argbSourceColor) == 0)
				{
					// fully transparent texels (IE the area around a fence post)
					// shouldn't overwrite anything behind them
					continue;
				}
				
				int pixelIndex = (pixelV * TEXTURE_WIDTH_AND_HEIGHT) + pixelU;
				pixels[pixelIndex] = blendSourceOver(argbSourceColor, pixels[pixelIndex]);
				anyPixelDrawn = true;
			}
		}
		return anyPixelDrawn;
	}
	
	/** standard alpha compositing, drawing the source color over the destination color */
	private static int blendSourceOver(int sourceArgb, int destArgb)
	{
		int sourceAlpha = ColorUtil.getAlpha(sourceArgb);
		if (sourceAlpha == 255)
		{
			return sourceArgb;
		}
		
		int destAlpha = ColorUtil.getAlpha(destArgb);
		int inverseSourceAlpha = 255 - sourceAlpha;
		
		int outAlpha = sourceAlpha + ((destAlpha * inverseSourceAlpha) / 255);
		if (outAlpha == 0)
		{
			return ColorUtil.INVISIBLE;
		}
		
		int outRed = ((ColorUtil.getRed(sourceArgb) * sourceAlpha) + ((ColorUtil.getRed(destArgb) * destAlpha * inverseSourceAlpha) / 255)) / outAlpha;
		int outGreen = ((ColorUtil.getGreen(sourceArgb) * sourceAlpha) + ((ColorUtil.getGreen(destArgb) * destAlpha * inverseSourceAlpha) / 255)) / outAlpha;
		int outBlue = ((ColorUtil.getBlue(sourceArgb) * sourceAlpha) + ((ColorUtil.getBlue(destArgb) * destAlpha * inverseSourceAlpha) / 255)) / outAlpha;
		return ColorUtil.argbToInt(outAlpha, outRed, outGreen, outBlue);
	}
	
	//endregion
	
	
	
	//===============//
	// quad decoding //
	//===============//
	//region
	
	/**
	 * A {@link BakedQuad} converted into the coordinate space of the face it's baked onto. <br><br>
	 *
	 * Face coordinates follow vanilla's block texture orientation:
	 * (0,0) is the face's top left corner when looking at the face from outside the block,
	 * with U increasing to the right and V increasing downward. <br>
	 * Depth increases toward the viewer, IE a depth of 1 touches the viewed face.
	 */
	private static class QuadGeometry
	{
		public final float[] faceU = new float[4];
		public final float[] faceV = new float[4];
		public final float[] depth = new float[4];
		
		/** sprite local texture coordinates in the range [0,1] */
		public final float[] spriteU = new float[4];
		public final float[] spriteV = new float[4];
		
		public TextureAtlasSprite sprite;
		public boolean tinted;
		
		public double getAverageDepth()
		{ return (this.depth[0] + this.depth[1] + this.depth[2] + this.depth[3]) / 4.0; }
	}
	
	private static QuadGeometry decodeQuad(BakedQuad quad, EDhDirection dhDirection)
	{
		QuadGeometry geometry = new QuadGeometry();
		
		geometry.sprite = getQuadSprite(quad);
		geometry.tinted = isQuadTinted(quad);
		
		for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++)
		{
			float x;
			float y;
			float z;
			float u;
			float v;
			
			#if MC_VER <= MC_1_12_2
			// 7 ints per vertex: x, y, z, color, u, v, lightmap
			int[] vertexData = quad.getVertexData();
			int vertexOffset = vertexIndex * 7;
			x = Float.intBitsToFloat(vertexData[vertexOffset]);
			y = Float.intBitsToFloat(vertexData[vertexOffset + 1]);
			z = Float.intBitsToFloat(vertexData[vertexOffset + 2]);
			u = Float.intBitsToFloat(vertexData[vertexOffset + 4]);
			v = Float.intBitsToFloat(vertexData[vertexOffset + 5]);
			#elif MC_VER <= MC_1_21_4
			// 8 ints per vertex: x, y, z, color, u, v, lightmap, normal
			int[] vertexData = quad.getVertices();
			int vertexOffset = vertexIndex * 8;
			x = Float.intBitsToFloat(vertexData[vertexOffset]);
			y = Float.intBitsToFloat(vertexData[vertexOffset + 1]);
			z = Float.intBitsToFloat(vertexData[vertexOffset + 2]);
			u = Float.intBitsToFloat(vertexData[vertexOffset + 4]);
			v = Float.intBitsToFloat(vertexData[vertexOffset + 5]);
			#elif MC_VER <= MC_1_21_11
			// 8 ints per vertex: x, y, z, color, u, v, lightmap, normal
			int[] vertexData = quad.vertices();
			int vertexOffset = vertexIndex * 8;
			x = Float.intBitsToFloat(vertexData[vertexOffset]);
			y = Float.intBitsToFloat(vertexData[vertexOffset + 1]);
			z = Float.intBitsToFloat(vertexData[vertexOffset + 2]);
			u = Float.intBitsToFloat(vertexData[vertexOffset + 4]);
			v = Float.intBitsToFloat(vertexData[vertexOffset + 5]);
			#else
			Vector3fc position = quad.position(vertexIndex);
			x = position.x();
			y = position.y();
			z = position.z();
			long packedUv = quad.packedUV(vertexIndex);
			u = UVPair.unpackU(packedUv);
			v = UVPair.unpackV(packedUv);
			#endif
			
			#if MC_VER <= MC_1_21_11
			// vertex UVs are texture atlas coordinates and
			// need to be converted into sprite local coordinates
			float minU = getSpriteMinU(geometry.sprite);
			float maxU = getSpriteMaxU(geometry.sprite);
			float minV = getSpriteMinV(geometry.sprite);
			float maxV = getSpriteMaxV(geometry.sprite);
			geometry.spriteU[vertexIndex] = (maxU != minU) ? ((u - minU) / (maxU - minU)) : 0.0f;
			geometry.spriteV[vertexIndex] = (maxV != minV) ? ((v - minV) / (maxV - minV)) : 0.0f;
			#else
			// vertex UVs are already sprite local coordinates,
			// the renderer looks the sprite up via the quad's material instead of the vertex data
			geometry.spriteU[vertexIndex] = u;
			geometry.spriteV[vertexIndex] = v;
			#endif
			
			projectOntoFace(dhDirection, x, y, z, geometry, vertexIndex);
		}
		
		return geometry;
	}
	
	/** Converts a block space position into the face space described in {@link QuadGeometry}. */
	private static void projectOntoFace(EDhDirection dhDirection, float x, float y, float z, QuadGeometry geometry, int vertexIndex)
	{
		float faceU;
		float faceV;
		float depth;
		switch (dhDirection)
		{
			case UP:
				faceU = x;
				faceV = z;
				depth = y;
				break;
			case DOWN:
				faceU = x;
				faceV = 1.0f - z;
				depth = 1.0f - y;
				break;
			case NORTH:
				faceU = 1.0f - x;
				faceV = 1.0f - y;
				depth = 1.0f - z;
				break;
			case SOUTH:
				faceU = x;
				faceV = 1.0f - y;
				depth = z;
				break;
			case WEST:
				faceU = z;
				faceV = 1.0f - y;
				depth = 1.0f - x;
				break;
			case EAST:
				faceU = 1.0f - z;
				faceV = 1.0f - y;
				depth = x;
				break;
			default:
				throw new IllegalArgumentException("No face projection for direction [" + dhDirection + "].");
		}
		
		geometry.faceU[vertexIndex] = faceU;
		geometry.faceV[vertexIndex] = faceV;
		geometry.depth[vertexIndex] = depth;
	}
	
	//endregion
	
	
	
	//====================//
	// minecraft wrappers //
	//====================//
	//region
	
	/**
	 * throws Exception MC may throw errors if this method is called on the wrong block (even though in that case it should just return null).
	 */
	@Nullable
	#if MC_VER <= MC_1_12_2
	private static List<BakedQuad> getQuads(IBlockState blockState, @Nullable EDhDirection dhDirection) throws Exception
	#else
	private static List<BakedQuad> getQuads(BlockState blockState, @Nullable EDhDirection dhDirection) throws Exception
	#endif
	{
		#if MC_VER <= MC_1_12_2
		EnumFacing direction = convertDirection(dhDirection);
		#else
		Direction direction = convertDirection(dhDirection);
		#endif
		
		List<BakedQuad> quads;
		
		#if MC_VER <= MC_1_12_2
		try
		{
			quads = MC.getBlockRendererDispatcher().getModelForState(blockState).getQuads(blockState, direction, RANDOM.nextLong());
		}
		catch (Exception e)
		{
			quads = Collections.emptyList();
		}
		#elif MC_VER < MC_1_21_5
		quads = MC.getModelManager().getBlockModelShaper().getBlockModel(blockState).getQuads(blockState, direction, RANDOM);
		#elif MC_VER <= MC_1_21_11
		List<BlockModelPart> blockModelPartList = MC.getModelManager().getBlockModelShaper().getBlockModel(blockState).collectParts(RANDOM);
		
		quads = new ArrayList<>();
		if (blockModelPartList != null)
		{
			for (int i = 0; i < blockModelPartList.size(); i++)
			{
				// if direction is null this will return the unculled quads
				quads.addAll(blockModelPartList.get(i).getQuads(direction));
			}
		}
		#else
		List<BlockStateModelPart> blockModelPartList = new ArrayList<>();
		MC.getModelManager().getBlockStateModelSet().get(blockState).collectParts(RANDOM, blockModelPartList);
		
		quads = new ArrayList<>();
		for (int i = 0; i < blockModelPartList.size(); i++)
		{
			// if direction is null this will return the unculled quads
			quads.addAll(blockModelPartList.get(i).getQuads(direction));
		}
		#endif
		
		return quads;
	}
	
	@Nullable
	#if MC_VER <= MC_1_12_2
	private static EnumFacing convertDirection(@Nullable EDhDirection dhDirection)
	#else
	private static Direction convertDirection(@Nullable EDhDirection dhDirection)
	#endif
	{
		if (dhDirection == null)
		{
			return null;
		}
		
		switch (dhDirection)
		{
			#if MC_VER <= MC_1_12_2
			case DOWN: return EnumFacing.DOWN;
			case UP: return EnumFacing.UP;
			case NORTH: return EnumFacing.NORTH;
			case SOUTH: return EnumFacing.SOUTH;
			case WEST: return EnumFacing.WEST;
			case EAST: return EnumFacing.EAST;
			#else
			case DOWN: return Direction.DOWN;
			case UP: return Direction.UP;
			case NORTH: return Direction.NORTH;
			case SOUTH: return Direction.SOUTH;
			case WEST: return Direction.WEST;
			case EAST: return Direction.EAST;
			#endif
			default: throw new IllegalArgumentException("No Minecraft direction for [" + dhDirection + "].");
		}
	}
	
	@Nullable
	private static TextureAtlasSprite getParticleSprite(BlockStateWrapper blockStateWrapper)
	{
		try
		{
			#if MC_VER <= MC_1_12_2
			return MC.getBlockRendererDispatcher().getBlockModelShapes().getTexture(blockStateWrapper.blockState);
			#elif MC_VER <= MC_1_21_11
			return MC.getModelManager().getBlockModelShaper().getParticleIcon(blockStateWrapper.blockState);
			#else
			return MC.getModelManager().getBlockStateModelSet().get(blockStateWrapper.blockState).particleMaterial().sprite();
			#endif
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to get particle sprite for block ["+blockStateWrapper.getSerialString()+"], error: ["+e.getMessage()+"].", e);
			return null;
		}
	}
	
	private static int getSpriteWidth(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getIconWidth();
		#elif MC_VER < MC_1_19_4
		return sprite.getWidth();
		#else
		return sprite.contents().width();
		#endif
	}
	private static int getSpriteHeight(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getIconHeight();
		#elif MC_VER < MC_1_19_4
		return sprite.getHeight();
		#else
		return sprite.contents().height();
		#endif
	}
	
	private static float getSpriteMinU(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getMinU();
		#else
		return sprite.getU0();
		#endif
	}
	private static float getSpriteMaxU(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getMaxU();
		#else
		return sprite.getU1();
		#endif
	}
	private static float getSpriteMinV(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getMinV();
		#else
		return sprite.getV0();
		#endif
	}
	private static float getSpriteMaxV(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getMaxV();
		#else
		return sprite.getV1();
		#endif
	}
	
	//endregion
	
	
	
	//====================//
	// debug file writing //
	//====================//
	//region
	
	private static void writeTopAndNorthTexturesToFile(BlockStateWrapper blockStateWrapper, BlockFaceTexture[] blockFaceTextures)
	{
		for (int i = 0; i < blockFaceTextures.length; i++)
		{
			// top and north are generally enough when troubleshooting texture problems,
			// although this can be commented out if additional directions need to be tested
			EDhDirection dir = FACE_DIRECTIONS[i];
			if (dir != EDhDirection.UP
				&& dir != EDhDirection.NORTH)
			{
				continue;
			}
			
			
			BlockFaceTexture faceTexture = blockFaceTextures[dir.faceIndex];
			
			String blockSerial = blockStateWrapper
				.getSerialString()
				.replace(":", "-")
				.replace("{", "[")
				.replace("}", "]")
				;
			String filePath = TEST_TEXTURE_OUTPUT_FOLDER_PATH + blockSerial + "_" + dir + ".png";
			try
			{
				writeArgbPixelsToPng(faceTexture.argbPixels, filePath);
			}
			catch (Exception e)
			{
				LOGGER.error("failed to save file ["+filePath+"], error: ["+e.getMessage()+"]");
			}
		}
	}
	
	public static void writeArgbPixelsToPng(int[] pixels, String outputPath)
		throws IOException
	{
		// Multiplies the output texture size by this many times.
		// Done to make viewing textures in File Explorer easier.
		int scale = 8;
		
		BufferedImage image = new BufferedImage(
			TEXTURE_WIDTH_AND_HEIGHT * scale,
			TEXTURE_WIDTH_AND_HEIGHT * scale, 
			BufferedImage.TYPE_INT_ARGB);
		
		for (int u = 0; u < TEXTURE_WIDTH_AND_HEIGHT; u++)
		{
			for (int v = 0; v < TEXTURE_WIDTH_AND_HEIGHT; v++)
			{
				int argb = pixels[(v * TEXTURE_WIDTH_AND_HEIGHT) + u];
				
				// duplicate the same pixel "scale" times to make it larger
				for (int uScale = 0; uScale < scale; uScale++)
				{
					for (int vScale = 0; vScale < scale; vScale++)
					{
						image.setRGB(
							(u * scale) + uScale, 
							(v * scale) + vScale, 
							argb);
					}
				}
			}
		}
		
		File outputFile = new File(outputPath);
		if (!ImageIO.write(image, "png", outputFile))
		{
			throw new IOException("No PNG writer found, javax.imageio may not be available.");
		}
	}
	
	//endregion
	
	

}
