package com.seibel.distanthorizons.cleanroom.mixins.client;

import com.seibel.distanthorizons.common.commonMixins.DhUpdateScreenBase;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft
{
	
	@Inject(method = "init", at = @At("TAIL"))
	private void onInit(CallbackInfo ci)
	{
		if(Config.Client.Advanced.AutoUpdater.enableAutoUpdater.get() && !ModInfo.IS_DEV_BUILD) // weird lib class not found error can occur if we don't check if we are in dev
		{
			DhUpdateScreenBase.tryShowUpdateScreenAndRunAutoUpdateStartup(null);
		}
	}
}
