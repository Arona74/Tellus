package com.seibel.distanthorizons.common.wrappers.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;

public class DhScreenUtil
{
	//================//
	// helper methods //
	//================//
	//region
	
	public static void showScreen(Screen screen)
	{
		#if MC_VER <= MC_26_1_2
		Objects.requireNonNull(Minecraft.getInstance()).setScreen(screen);
		#else
		Objects.requireNonNull(Minecraft.getInstance()).setScreenAndShow(screen);
		#endif
	}
	
	//endregion
	
	
	
}
