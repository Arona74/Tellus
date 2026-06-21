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

package com.seibel.distanthorizons.common.render.openGl.terrain;

import com.seibel.distanthorizons.core.dataObjects.render.textures.BlockTextureRegistry;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;

/**
 * The GPU side of the {@link BlockTextureRegistry},
 * a texture array with one layer per block face tile. <br><br>
 *
 * Layer {@link BlockTextureRegistry#FLAT_TILE_ID} is a uniform 1.0 color multiplier
 * so vertices without a texture render exactly like flat-colored LODs.
 *
 * @see BlockTextureRegistry
 */
public class GlBlockTextureAtlas
{
	/** the texture unit the atlas is bound to while LODs render, the lightmap uses unit 0 */
	public static final int GL_BOUND_INDEX = 1;

	/** how many layers the atlas starts with and grows by, 1KB per layer */
	private static final int ALLOCATION_LAYER_COUNT = 1024;

	private int textureId = 0;
	private int allocatedLayerCount = 0;
	private int uploadedTileCount = 0;



	//===========//
	// uploading //
	//===========//

	/**
	 * Uploads any newly registered tiles to the GPU,
	 * must be called on the render thread before LODs are rendered.
	 */
	public void uploadPendingTiles()
	{
		int totalTileCount = BlockTextureRegistry.INSTANCE.getTileCount();
		if (totalTileCount == this.uploadedTileCount && this.textureId != 0)
		{
			return;
		}

		if (totalTileCount > this.allocatedLayerCount || this.textureId == 0)
		{
			this.growAtlas(totalTileCount);
		}

		BlockTextureRegistry.PendingTiles pendingTiles = BlockTextureRegistry.INSTANCE.getAndClearPendingUploadTiles();
		if (pendingTiles == null)
		{
			return;
		}

		GL32.glBindTexture(GL32.GL_TEXTURE_2D_ARRAY, this.textureId);
		ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(BlockTextureRegistry.TILE_BYTE_COUNT);
		for (int i = 0; i < pendingTiles.tilePixels.length; i++)
		{
			pixelBuffer.clear();
			pixelBuffer.put(pendingTiles.tilePixels[i]);
			pixelBuffer.flip();

			GL32.glTexSubImage3D(GL32.GL_TEXTURE_2D_ARRAY, 0,
					0, 0, pendingTiles.firstTileId + i,
					BlockTextureRegistry.TILE_WIDTH, BlockTextureRegistry.TILE_WIDTH, 1,
					GL32.GL_RGBA, GL32.GL_UNSIGNED_BYTE, pixelBuffer);
		}
		this.uploadedTileCount = pendingTiles.firstTileId + pendingTiles.tilePixels.length;
	}

	private void growAtlas(int minLayerCount)
	{
		int newLayerCount = Math.max(this.allocatedLayerCount, ALLOCATION_LAYER_COUNT);
		while (newLayerCount < minLayerCount)
		{
			newLayerCount *= 2;
		}

		int newTextureId = GL32.glGenTextures();
		GL32.glBindTexture(GL32.GL_TEXTURE_2D_ARRAY, newTextureId);
		GL32.glTexImage3D(GL32.GL_TEXTURE_2D_ARRAY, 0, GL32.GL_RGBA8,
				BlockTextureRegistry.TILE_WIDTH, BlockTextureRegistry.TILE_WIDTH, newLayerCount,
				0, GL32.GL_RGBA, GL32.GL_UNSIGNED_BYTE, (ByteBuffer) null);

		// nearest filtering keeps the blocky look and prevents
		// texels bleeding between unrelated tiles on adjacent layers
		GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
		GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
		GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_WRAP_S, GL32.GL_REPEAT);
		GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_WRAP_T, GL32.GL_REPEAT);
		GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_MAX_LEVEL, 0);

		if (this.textureId != 0)
		{
			GL32.glDeleteTextures(this.textureId);
		}
		this.textureId = newTextureId;
		this.allocatedLayerCount = newLayerCount;
		// all tiles need to be re-uploaded into the new texture
		this.uploadedTileCount = 0;
		BlockTextureRegistry.INSTANCE.resetPendingUploads();
	}



	//=========//
	// binding //
	//=========//

	public void bind()
	{
		GL32.glActiveTexture(GL32.GL_TEXTURE0 + GL_BOUND_INDEX);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D_ARRAY, this.textureId);
		GL32.glActiveTexture(GL32.GL_TEXTURE0);
	}

	public void unbind()
	{
		GL32.glActiveTexture(GL32.GL_TEXTURE0 + GL_BOUND_INDEX);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D_ARRAY, 0);
		GL32.glActiveTexture(GL32.GL_TEXTURE0);
	}

	public void free()
	{
		if (this.textureId != 0)
		{
			GL32.glDeleteTextures(this.textureId);
			this.textureId = 0;
			this.allocatedLayerCount = 0;
			this.uploadedTileCount = 0;
		}
	}

}
