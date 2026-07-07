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
import com.seibel.distanthorizons.core.render.AbstractBlockTextureAtlas;
import org.lwjgl.opengl.GL32;

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
public class BlazeBlockTextureAtlas extends AbstractBlockTextureAtlas
{
	public static final BlazeBlockTextureAtlas INSTANCE = new BlazeBlockTextureAtlas();
	
	private final BlazeTextureWrapper textureWrapper = BlazeTextureWrapper.createTextureAtlas("BlockTextureAtlas");
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private BlazeBlockTextureAtlas() { }
	
	//endregion
	
	
	
	//==================//
	// texture handling //
	//==================//
	//region
	
	@Override 
	protected void tryCreateOrResize(int width, int height) { textureWrapper.tryCreateOrResize(width, height); }
	
	public IDhBlazeTexture getTextureWrapper() { return this.textureWrapper; }
	
	//endregion
	
	
	
	//===========//
	// uploading //
	//===========//
	//region
	
	@Override
	protected void beforeWriteToTexture() { /* no setup required */ }
	
	@Override 
	protected void writeToTexture(ByteBuffer pixelBuffer, int destinationX, int destinationY, int tileWidth, int tileHeight)
	{
		this.textureWrapper.writeToTexture(
			pixelBuffer,
			destinationX, // x
			destinationY, // y
			BlockTextureRegistry.TILE_HEIGHT_AND_WIDTH, // width
			BlockTextureRegistry.TILE_HEIGHT_AND_WIDTH // height
		);
	}
	
	@Override
	protected void afterWriteToTexture() { /* no cleanup required */ }
	
	//endregion
	
	
	
}
#endif
