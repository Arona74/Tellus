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

package com.seibel.distanthorizons.common.render.blaze.wrappers.texture;

#if MC_VER <= MC_1_21_10
public class BlazeBlockTextureAtlas {}

#else

import com.seibel.distanthorizons.core.dataObjects.render.textures.BlockTextureRegistry;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList.PhantomArrayListCheckout;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList.PhantomArrayListPool;

import java.nio.ByteBuffer;

/**
 * The Blaze3D equivalent of the OpenGL block texture atlas. <br><br>
 *
 * Blaze3D doesn't support array textures yet, so unlike the OpenGL atlas
 * the tiles are packed into a 2D grid texture
 * with {@link BlazeBlockTextureAtlas#TILES_PER_ROW} tiles per row,
 * growing downward as more tiles are registered.
 * The fragment shader locates a tile via its id and the texture's size,
 * so no extra uniforms are needed.
 *
 * @see BlockTextureRegistry
 * @see com.seibel.distanthorizons.common.render.openGl.terrain.GlBlockTextureAtlas
 */
public class BlazeBlockTextureAtlas
{
	public static final BlazeBlockTextureAtlas INSTANCE = new BlazeBlockTextureAtlas();
	
	/**
	 * Must match the tile lookup in the Blaze LOD fragment shader. <br>
	 * 256 tiles * 16 pixels = a constant 4096 pixel wide texture,
	 * holding every possible tile id at 4096 pixels tall.
	 */
	public static final int TILES_PER_ROW = 256;
	
	/** how many tile rows the atlas starts with and grows by, 1MB of texture per step */
	private static final int ALLOCATION_ROW_COUNT = 16;
	
	private static final PhantomArrayListPool ARRAY_LIST_POOL = new PhantomArrayListPool("BlazeBlockTextureAtlas");
	
	
	private final BlazeTextureWrapper textureWrapper = BlazeTextureWrapper.createTextureAtlas("BlockTextureAtlas");
	private int allocatedTileCount = 0;
	private int uploadedTileCount = 0;
	
	
	
	//===========//
	// uploading //
	//===========//
	//region
	
	/**
	 * Uploads any newly registered tiles to the GPU,
	 * must be called on the render thread outside an active render pass.
	 */
	public void uploadPendingTiles()
	{
		int totalTileCount = BlockTextureRegistry.INSTANCE.getTileCount();
		if (totalTileCount == this.uploadedTileCount)
		{
			return;
		}
		
		if (totalTileCount > this.allocatedTileCount)
		{
			this.growAtlas(totalTileCount);
		}
		
		BlockTextureRegistry.PendingTiles pendingTiles = BlockTextureRegistry.INSTANCE.getAndClearPendingUploadTiles();
		if (pendingTiles == null)
		{
			return;
		}
		
		try(PhantomArrayListCheckout checkout = ARRAY_LIST_POOL.checkoutByteBuffers(1))
		{
			ByteBuffer pixelBuffer = checkout.getByteBuffer(0, BlockTextureRegistry.TILE_BYTE_COUNT);
			for (int i = 0; i < pendingTiles.tilePixels.length; i++)
			{
				pixelBuffer.clear();
				pixelBuffer.put(pendingTiles.tilePixels[i]);
				pixelBuffer.flip();
				
				int tileId = pendingTiles.firstTileId + i;
				this.textureWrapper.writeToTexture(
					pixelBuffer,
					(tileId % TILES_PER_ROW) * BlockTextureRegistry.TILE_WIDTH, // x
					(tileId / TILES_PER_ROW) * BlockTextureRegistry.TILE_WIDTH, // y
					BlockTextureRegistry.TILE_WIDTH, // width
					BlockTextureRegistry.TILE_WIDTH	// height
				);
			}
		}
		this.uploadedTileCount = pendingTiles.firstTileId + pendingTiles.tilePixels.length;
	}
	
	private void growAtlas(int minTileCount)
	{
		int newRowCount = Math.max(this.allocatedTileCount / TILES_PER_ROW, ALLOCATION_ROW_COUNT);
		while (newRowCount * TILES_PER_ROW < minTileCount)
		{
			newRowCount *= 2;
		}
		
		int width = TILES_PER_ROW * BlockTextureRegistry.TILE_WIDTH;
		int height = newRowCount * BlockTextureRegistry.TILE_WIDTH;
		textureWrapper.tryCreateOrResize(width, height);
		
		this.allocatedTileCount = newRowCount * TILES_PER_ROW;
		// all tiles need to be re-uploaded into the new texture
		this.uploadedTileCount = 0;
		BlockTextureRegistry.INSTANCE.resetPendingUploads();
	}
	
	//endregion
	
	
	public IDhBlazeTexture getTextureWrapper() { return this.textureWrapper; }
	
	
	
}
#endif
