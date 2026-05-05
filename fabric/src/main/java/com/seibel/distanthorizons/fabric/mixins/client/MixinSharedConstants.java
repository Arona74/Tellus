package com.seibel.distanthorizons.fabric.mixins.client;

#if MC_VER <= MC_1_21_10
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public class MixinSharedConstants
{ /* not present in older MC versions */ }
#else

import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SharedConstants.class)
public abstract class MixinSharedConstants
{
	@Mutable
	@Shadow @Final public static boolean IS_RUNNING_IN_IDE;
	
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void setIsRunningInIde(CallbackInfo ci) 
	{
		DhLogger logger = new DhLoggerBuilder().name("SharedConstants").build();
		
		// setting IS_RUNNING_IN_IDE to true enables
		// additional validation on Mojang's side which
		// helps catch errors when developing for Blaze3D
		
		boolean irisPresent;
		#if MC_VER <= MC_1_21_11
		IS_RUNNING_IN_IDE = ModInfo.IS_DEV_BUILD;
		#else
		try
		{
			// Iris has a bug for MC 26 and newer where it doesn't have
			// a "sampler1" bound, causing a renderer crash if
			// Blaze3D validation is enabled (which is enabled by if
			// IS_RUNNING_IN_IDE is true)
			ModInfo.class.getClassLoader().loadClass("net.irisshaders.iris.api.v0.IrisApi");
			irisPresent = true;
		}
		catch (ClassNotFoundException ignore)
		{
			irisPresent = false;
		}
		
		IS_RUNNING_IN_IDE = ModInfo.IS_DEV_BUILD && !irisPresent;
		#endif
		
		logger.info("Setting Minecraft's SharedConstants.IS_RUNNING_IN_IDE to ["+IS_RUNNING_IN_IDE+"]");
	}
	
}
#endif
