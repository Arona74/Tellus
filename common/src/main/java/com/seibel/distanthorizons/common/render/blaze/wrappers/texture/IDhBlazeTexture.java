package com.seibel.distanthorizons.common.render.blaze.wrappers.texture;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

public interface IDhBlazeTexture
{
	
	GpuTextureView getTextureView();
	GpuSampler getTextureSampler();
	
}
