package com.seibel.distanthorizons.common.wrappers.gui;

#if MC_VER <= MC_1_12_2
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import java.util.HashMap;
import java.util.Map;
#else
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
#endif

#if MC_VER < MC_1_19_2 && MC_VER > MC_1_12_2
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
#endif

public class GuiHelper
{
	/**
	 * Helper static methods for versional compat
	 */
	#if MC_VER <= MC_1_12_2
	public static final Map<GuiButton, OnPressed> HANDLER_BY_BUTTON = new HashMap<>();
	#endif
	
	public static #if MC_VER <= MC_1_12_2 GuiButton #else Button #endif MakeBtn(#if MC_VER <= MC_1_12_2 ITextComponent #else Component #endif base, int posX, int posZ, int width, int height,
		#if MC_VER <= MC_1_12_2 OnPressed #else Button.OnPress #endif action)
	{
		#if MC_VER <= MC_1_12_2
		GuiButton button = new GuiButton(HANDLER_BY_BUTTON.size(), posX, posZ, width, height, base.getFormattedText());
		HANDLER_BY_BUTTON.put(button, action);
		return button;
        #elif MC_VER < MC_1_19_4
		return new Button(posX, posZ, width, height, base, action);
        #else
		return Button.builder(base, action).bounds(posX, posZ, width, height).build();
        #endif
	}
	
	public static #if MC_VER <= MC_1_12_2 ITextComponent #else MutableComponent #endif TextOrLiteral(String text)
	{
		#if MC_VER <= MC_1_12_2
		return new TextComponentString(text);
        #elif MC_VER < MC_1_19_2
		return new TextComponent(text);
        #else
		return Component.literal(text);
        #endif
	}
	
	public static #if MC_VER <= MC_1_12_2 ITextComponent #else MutableComponent #endif TextOrTranslatable(String text)
	{
		#if MC_VER <= MC_1_12_2
		return new TextComponentString(text);
        #elif MC_VER < MC_1_19_2
		return new TextComponent(text);
        #else
		return Component.translatable(text);
        #endif
	}
	
	public static #if MC_VER <= MC_1_12_2 ITextComponent #else MutableComponent #endif Translatable(String text, Object... args)
	{
		#if MC_VER <= MC_1_12_2
		return new TextComponentTranslation(text, args);
        #elif MC_VER < MC_1_19_2
		return new TranslatableComponent(text, args);
        #else
		return Component.translatable(text, args);
        #endif
	}
	
	public static void SetX(#if MC_VER <= MC_1_12_2 GuiButton #else AbstractWidget #endif w, int x)
	{
        #if MC_VER < MC_1_19_4
		w.x = x;
        #else
		w.setX(x);
        #endif
	}
	
	#if MC_VER <= MC_1_12_2
	public static void SetY(GuiTextField w, int y)
	{
		w.y = y;
	}
	#endif
	
	public static void SetY(#if MC_VER <= MC_1_12_2 GuiButton #else AbstractWidget #endif w, int y)
	{
        #if MC_VER < MC_1_19_4
		w.y = y;
        #else
		w.setY(y);
        #endif
	}
	
}
