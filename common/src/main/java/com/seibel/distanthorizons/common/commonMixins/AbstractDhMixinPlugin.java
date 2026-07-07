package com.seibel.distanthorizons.common.commonMixins;

import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IImmersivePortalsAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IModChecker;

public abstract class AbstractDhMixinPlugin
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	public boolean shouldApplyMixin(IModChecker modChecker, String targetClassName, String mixinClassName)
	{
		if (mixinClassName.endsWith("MixinImmersivePortalsRenderStates"))
		{
			boolean immersivePortalsPresent = modChecker.isModLoaded(IImmersivePortalsAccessor.CORE_MOD_ID)
				|| modChecker.isModLoaded(IImmersivePortalsAccessor.MOD_ID);
			
			if (!immersivePortalsPresent)
			{
				LOGGER.info("Immersive Portals not present, skipping DH compatibility mixin.");
			}
			
			return immersivePortalsPresent;
		}
		
		return true;
	}
	
}
