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

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.distanthorizons.core.dataObjects.render.textures.BlockTextureRegistry;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;

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


	private final TextureWrapper textureWrapper = new TextureWrapper();

	private GpuTexture texture = null;
	private GpuTextureView textureView = null;
	private GpuSampler sampler = null;
	private int allocatedTileCount = 0;
	private int uploadedTileCount = 0;



	//===========//
	// uploading //
	//===========//

	/**
	 * Uploads any newly registered tiles to the GPU,
	 * must be called on the render thread outside an active render pass.
	 */
	public void uploadPendingTiles()
	{
		int totalTileCount = BlockTextureRegistry.INSTANCE.getTileCount();
		if (totalTileCount == this.uploadedTileCount && this.texture != null)
		{
			return;
		}

		if (totalTileCount > this.allocatedTileCount || this.texture == null)
		{
			this.growAtlas(totalTileCount);
		}

		BlockTextureRegistry.PendingTiles pendingTiles = BlockTextureRegistry.INSTANCE.getAndClearPendingUploadTiles();
		if (pendingTiles == null)
		{
			return;
		}

		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(BlockTextureRegistry.TILE_BYTE_COUNT);
		for (int i = 0; i < pendingTiles.tilePixels.length; i++)
		{
			pixelBuffer.clear();
			pixelBuffer.put(pendingTiles.tilePixels[i]);
			pixelBuffer.flip();

			int tileId = pendingTiles.firstTileId + i;
			commandEncoder.writeToTexture(this.texture, pixelBuffer,
					NativeImage.Format.RGBA,
					/*mipLevel*/ 0, /*depthOrLayer*/ 0,
					/*x*/ (tileId % TILES_PER_ROW) * BlockTextureRegistry.TILE_WIDTH,
					/*y*/ (tileId / TILES_PER_ROW) * BlockTextureRegistry.TILE_WIDTH,
					BlockTextureRegistry.TILE_WIDTH, BlockTextureRegistry.TILE_WIDTH);
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

		GpuDevice device = RenderSystem.getDevice();

		if (this.textureView != null)
		{
			this.textureView.close();
		}
		if (this.texture != null)
		{
			this.texture.close();
		}

		this.texture = device.createTexture("DH block texture atlas",
				GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
				TextureFormat.RGBA8,
				TILES_PER_ROW * BlockTextureRegistry.TILE_WIDTH,
				newRowCount * BlockTextureRegistry.TILE_WIDTH,
				/*depthOrLayers*/ 1, /*mipLevels*/ 1);
		this.textureView = device.createTextureView(this.texture);

		if (this.sampler == null)
		{
			// nearest filtering keeps the blocky look and prevents
			// texels bleeding between adjacent tiles in the grid
			this.sampler = device.createSampler(
					AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
					FilterMode.NEAREST, FilterMode.NEAREST,
					/*maxAnisotropy*/ 1, OptionalDouble.empty());
		}

		this.allocatedTileCount = newRowCount * TILES_PER_ROW;
		// all tiles need to be re-uploaded into the new texture
		this.uploadedTileCount = 0;
		BlockTextureRegistry.INSTANCE.resetPendingUploads();
	}

	public IDhBlazeTexture getTextureWrapper() { return this.textureWrapper; }

	/** exposes the atlas through the same interface the lightmap uses for render pass binding */
	private class TextureWrapper implements IDhBlazeTexture
	{
		@Override
		public GpuTextureView getTextureView() { return BlazeBlockTextureAtlas.this.textureView; }

		@Override
		public GpuSampler getTextureSampler() { return BlazeBlockTextureAtlas.this.sampler; }

	}

}
#endif
