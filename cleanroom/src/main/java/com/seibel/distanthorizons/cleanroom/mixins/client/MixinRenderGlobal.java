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

package com.seibel.distanthorizons.cleanroom.mixins.client;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.util.math.DhMat4f;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import org.lwjgl.opengl.GL32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal
{
	@Shadow private WorldClient world;

	@Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", at = @At("HEAD"))
	private void renderChunkLayer(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn, CallbackInfoReturnable<Integer> cir)
	{
		if (blockLayerIn == BlockRenderLayer.SOLID)
		{
			float[] mcProjMatrixRaw = new float[16];
			GL32.glGetFloatv(GL32.GL_PROJECTION_MATRIX, mcProjMatrixRaw);
			ClientApi.RENDER_STATE.mcProjectionMatrix = new DhMat4f(mcProjMatrixRaw);
			ClientApi.RENDER_STATE.mcProjectionMatrix.transpose();
			
			float[] mcModelViewRaw = new float[16];
			GL32.glGetFloatv(GL32.GL_MODELVIEW_MATRIX, mcModelViewRaw);
			ClientApi.RENDER_STATE.mcModelViewMatrix = new DhMat4f(mcModelViewRaw);
			ClientApi.RENDER_STATE.mcModelViewMatrix.transpose();
			
			ClientApi.RENDER_STATE.partialTickTime = (float) partialTicks;
			ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapperIfDifferent(ClientApi.RENDER_STATE.clientLevelWrapper, this.world);
			
			ClientApi.INSTANCE.renderLods();
			
			//Some 1.12.2 rendering mods breaks if we don't unbind buffers
			GL32.glBindVertexArray(0);
			GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, 0);
			GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, 0);
			GL32.glUseProgram(0);
		}
	}
}