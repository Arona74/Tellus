package com.seibel.distanthorizons.common.wrappers.minecraft;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import org.jetbrains.annotations.Nullable;


#if MC_VER > MC_1_12_2
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
#endif

#if  MC_VER <= MC_1_12_2
#elif  MC_VER <= MC_1_21_10
import net.minecraft.resources.ResourceLocation;
#else
import net.minecraft.resources.Identifier;
#endif

#if  MC_VER > MC_1_19_2
import net.minecraft.core.registries.Registries;
#elif MC_VER > MC_1_12_2
import net.minecraft.core.Registry;
#endif

public abstract class AbstractMinecraftSharedWrapper implements IMinecraftSharedWrapper
{
	
	@Nullable
	#if MC_VER <= MC_1_12_2
	protected Integer deserializeDimensionResourceKey(String dimensionResourceLocation)
	#else
	protected ResourceKey<Level> deserializeDimensionResourceKey(String dimensionResourceLocation)
	#endif
	{
		#if  MC_VER <= MC_1_12_2
		try
		{
			return Integer.parseInt(dimensionResourceLocation.substring(dimensionResourceLocation.indexOf(":")+1));
		}
		catch (NumberFormatException ignored)
		{
			return null;
		}
		#else
			#if  MC_VER <= MC_1_21_10
			ResourceLocation dimResourceLocation = ResourceLocation.tryParse(dimensionResourceLocation);
			#else
			Identifier dimResourceLocation = Identifier.tryParse(dimensionResourceLocation);
			#endif
			if (dimResourceLocation == null)
			{
				return null;
			}
			
			#if  MC_VER > MC_1_19_2
			ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimResourceLocation);
			#else
			ResourceKey<Level> dimensionKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimResourceLocation);
			#endif
			
		return dimensionKey;
		#endif
	}
	
}
