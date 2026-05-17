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

package com.seibel.distanthorizons.common.render.blaze.postProcessing;

#if MC_VER <= MC_1_21_10
public class BlazeVanillaFadeRenderer {}

#else
	
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seibel.distanthorizons.common.render.blaze.BlazeDhMetaRenderer;
import com.seibel.distanthorizons.common.render.blaze.apply.BlazeDhCopyRenderer;
import com.seibel.distanthorizons.common.render.blaze.util.BlazePostProcessUtil;
import com.seibel.distanthorizons.common.render.blaze.util.BlazeUniformUtil;
import com.seibel.distanthorizons.common.render.blaze.wrappers.RenderPassWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.RenderPipelineBuilderWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.texture.BlazeTextureViewWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.texture.BlazeTextureWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.EDhRenderApi;
import com.seibel.distanthorizons.core.render.EDhRenderDepth;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.AbstractDhRenderApiDefinition;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhVanillaFadeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Fades the vanilla chunks
 * into DH's LODs.
 */
public class BlazeVanillaFadeRenderer implements IDhVanillaFadeRenderer
{
	public static final DhLogger LOGGER = new DhLoggerBuilder().build(); 
	
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final AbstractDhRenderApiDefinition RENDER_API_DEF = SingletonInjector.INSTANCE.get(AbstractDhRenderApiDefinition.class);
	
	private static final GpuDevice GPU_DEVICE = RenderSystem.getDevice();
	private static final CommandEncoder COMMAND_ENCODER = GPU_DEVICE.createCommandEncoder();
	
	public static final BlazeVanillaFadeRenderer INSTANCE = new BlazeVanillaFadeRenderer();
	
	
	private RenderPipeline pipeline;
	private boolean init = false;
	
	private GpuBuffer fragUniformBuffer;
	
	private GpuBuffer vboGpuBuffer;
	
	public final BlazeTextureWrapper fadeColorTextureWrapper = BlazeTextureWrapper.createColor("DhVanillaFadeColorTexture");
	/** We don't want to actually write any depth data, but blaze3D complains if we don't bind a depth texture. */
	private final BlazeTextureWrapper fadeDepthTextureWrapper = BlazeTextureWrapper.createDepth("DhVanillaFadeDepthTexture");
	
	
	public final BlazeTextureViewWrapper mcDepthTextureWrapper = new BlazeTextureViewWrapper();
	public final BlazeTextureViewWrapper mcColorTextureWrapper = new BlazeTextureViewWrapper();
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private BlazeVanillaFadeRenderer() { }
	
	private void tryInit()
	{
		if (this.init)
		{
			return;
		}
		this.init = true;
		
		
		
		RenderPipelineBuilderWrapper pipelineBuilder = new RenderPipelineBuilderWrapper();
		{
			pipelineBuilder.withFaceCulling(false);
			pipelineBuilder.withDepthWrite(false);
			pipelineBuilder.withDepthTest(RenderPipelineBuilderWrapper.EDhDepthTest.NONE);
			pipelineBuilder.withColorWrite(true);
			pipelineBuilder.withoutBlend();
			pipelineBuilder.withPolygonMode(RenderPipelineBuilderWrapper.EDhPolygonMode.FILL);
			pipelineBuilder.withName("vanilla_fade");
			
			pipelineBuilder.withVertexShader("fade/blaze/vert");
			pipelineBuilder.withFragmentShader("fade/blaze/vanilla_fade");
			
			pipelineBuilder.withSampler("uMcDepthTexture");
			pipelineBuilder.withSampler("uCombinedMcDhColorTexture");
			
			pipelineBuilder.withSampler("uDhDepthTexture");
			pipelineBuilder.withSampler("uDhColorTexture");
			
			pipelineBuilder.withUniformBuffer("fragUniformBlock");
			
			pipelineBuilder.withVertexFormat(BlazePostProcessUtil.createVertexFormat());
			pipelineBuilder.withVertexMode(RenderPipelineBuilderWrapper.EDhVertexMode.TRIANGLE_FAN);
		}
		this.pipeline = pipelineBuilder.build();
		
		
		this.vboGpuBuffer = BlazePostProcessUtil.createAndUploadScreenVertexData("McFadeRenderer");
	}
	
	//endregion
	
	
	
	//========//
	// render //
	//========//
	//region
	
	@Override
	public void render(RenderParams renderParams)
	{
		this.tryInit();
		
		if (BlazeDhMetaRenderer.INSTANCE.dhDepthTextureWrapper.isEmpty()
			|| BlazeDhMetaRenderer.INSTANCE.dhColorTextureWrapper.isEmpty())
		{
			return;	
		}
		
		
		// textures
		this.fadeColorTextureWrapper.tryCreateOrResize();
		this.fadeDepthTextureWrapper.tryCreateOrResize();
		
		this.mcDepthTextureWrapper.tryWrap(MinecraftRenderWrapper.INSTANCE.getRenderTarget().getDepthTexture());
		this.mcColorTextureWrapper.tryWrap(MinecraftRenderWrapper.INSTANCE.getRenderTarget().getColorTexture());
		
		
		{
			int uniformBufferSize = new Std140SizeCalculator()
				.putInt() // uOnlyRenderLods
				.putFloat() // uStartFadeBlockDistance
				.putFloat() // uEndFadeBlockDistance
				.putFloat() // uMaxLevelHeight
				.putMat4f() // uDhInvMvmProj
				.putMat4f() // uMcInvMvmProj
				.putInt() // uIsReverseZDepth
				.get();
			
			
			// create data //
			
			float dhNearClipDistance = RenderUtil.getNearClipPlaneInBlocks();
			// this added value prevents the near clip plane and discard circle from touching, which looks bad
			dhNearClipDistance += 16f;
			
			// measured in blocks
			// these multipliers in James' tests should provide a fairly smooth transition
			// without having underdraw issues
			float fadeStartDistance = dhNearClipDistance * 1.5f;
			float fadeEndDistance = dhNearClipDistance * 1.9f;
			
			
			Mat4f inverseMcModelViewProjectionMatrix = new Mat4f(renderParams.mcProjectionMatrix);
			inverseMcModelViewProjectionMatrix.multiply(renderParams.mcModelViewMatrix);
			inverseMcModelViewProjectionMatrix.invert();
			Mat4f inverseMcMvmProjMatrix = inverseMcModelViewProjectionMatrix;
			
			
			Mat4f inverseDhModelViewProjectionMatrix = new Mat4f(renderParams.dhProjectionMatrix);
			inverseDhModelViewProjectionMatrix.multiply(renderParams.dhModelViewMatrix);
			inverseDhModelViewProjectionMatrix.invert();
			Mat4f inverseDhMvmProjMatrix = inverseDhModelViewProjectionMatrix;
			
			
			
			// upload data //
			
			ByteBuffer buffer = ByteBuffer.allocateDirect(uniformBufferSize);
			buffer.order(ByteOrder.nativeOrder());
			buffer = Std140Builder.intoBuffer(buffer)
				.putInt(Config.Client.Advanced.Debugging.lodOnlyMode.get() ? 1 : 0) // uOnlyRenderLods
				.putFloat(fadeStartDistance) // uStartFadeBlockDistance
				.putFloat(fadeEndDistance) // uEndFadeBlockDistance
				.putFloat(renderParams.clientLevelWrapper.getMaxHeight()) // uMaxLevelHeight
				.putMat4f(inverseDhMvmProjMatrix.createJomlMatrix()) // uDhInvMvmProj
				.putMat4f(inverseMcMvmProjMatrix.createJomlMatrix()) // uMcInvMvmProj
				.putInt((RENDER_API_DEF.getRenderDepth() == EDhRenderDepth.REVERSE_Z) ? 1 : 0) // uIsReverseZDepth
				.get()
			;
			
			this.fragUniformBuffer = BlazeUniformUtil.createBuffer("fragUniformBlock", uniformBufferSize, this.fragUniformBuffer);
			GpuBufferSlice bufferSlice = new GpuBufferSlice(this.fragUniformBuffer, 0, uniformBufferSize);
			
			COMMAND_ENCODER.writeToBuffer(bufferSlice, buffer);
		}
		
		
		this.renderFadeToTexture();
		BlazeDhCopyRenderer.INSTANCE.render(this.fadeColorTextureWrapper, this.mcColorTextureWrapper);
		
	}
	
	private void renderFadeToTexture()
	{
		try (RenderPassWrapper renderPassWrapper = new RenderPassWrapper(
			this::getRenderPassName,
			this.fadeColorTextureWrapper,
			this.fadeDepthTextureWrapper))
		{
			renderPassWrapper.bindTexture("uMcDepthTexture", this.mcDepthTextureWrapper);
			renderPassWrapper.bindTexture("uCombinedMcDhColorTexture", this.mcColorTextureWrapper);
			
			renderPassWrapper.bindTexture("uDhDepthTexture", BlazeDhMetaRenderer.INSTANCE.dhDepthTextureWrapper);
			renderPassWrapper.bindTexture("uDhColorTexture", BlazeDhMetaRenderer.INSTANCE.dhColorTextureWrapper);
			
			renderPassWrapper.renderPass.setUniform("fragUniformBlock", this.fragUniformBuffer);
			
			renderPassWrapper.setVertexBuffer(this.vboGpuBuffer);
			
			renderPassWrapper.renderPass.setPipeline(this.pipeline);
			renderPassWrapper.draw(/*indexCount*/ 4);
		}
	}
	private String getRenderPassName() { return "distantHorizons:McFadeRenderer"; }
	
	//endregion
	
	
	
}
#endif