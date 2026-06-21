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
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateFaceTextureProvider;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes {@link ClientBlockStateTextureCache} to DH core.
 *
 * @see IBlockStateFaceTextureProvider
 */
public class BlockStateFaceTextureProvider implements IBlockStateFaceTextureProvider
{
	public static final BlockStateFaceTextureProvider INSTANCE = new BlockStateFaceTextureProvider();

	@Nullable
	@Override
	public BlockFaceTexture getFaceTexture(IBlockStateWrapper blockState, EDhDirection direction)
	{
		if (!(blockState instanceof BlockStateWrapper))
		{
			// API users can provide their own IBlockStateWrapper implementations
			// which don't have Minecraft block states to bake
			// TODO: (need to double check)
			return null;
		}
		return ClientBlockStateTextureCache.getFaceTexture((BlockStateWrapper) blockState, direction);
	}

	@Override
	public void clearCache() { ClientBlockStateTextureCache.clearCache(); }

}
