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

package com.seibel.distanthorizons.common.wrappers.minecraft;

import java.io.File;

#if MC_VER > MC_1_12_2
import com.mojang.blaze3d.platform.Window;
#endif
import com.seibel.distanthorizons.common.wrappers.gui.NativeDialogUtil;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.file.structure.ClientOnlySaveStructure;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.logging.DhLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
#if MC_VER <= MC_1_12_2
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.crash.CrashReport;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
#else
import net.minecraft.CrashReport;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
#endif

import org.jetbrains.annotations.Nullable;

#if MC_VER < MC_1_19_2 && MC_VER > MC_1_12_2
import net.minecraft.network.chat.TextComponent;
#endif

#if MC_VER < MC_1_21_3
#else
import net.minecraft.util.profiling.Profiler;
#endif

#if MC_VER <= MC_1_21_10 && MC_VER > MC_1_12_2
import net.minecraft.client.GraphicsStatus;
#else
#endif


/**
 * A singleton that wraps the Minecraft object.
 *
 * @author James Seibel
 */
public class MinecraftClientWrapper implements IMinecraftClientWrapper, IMinecraftSharedWrapper
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final Minecraft MINECRAFT = Minecraft.#if MC_VER <= MC_1_12_2 getMinecraft() #else getInstance() #endif;
	
	public static final MinecraftClientWrapper INSTANCE = new MinecraftClientWrapper();
	
	
	private ProfilerWrapper profilerWrapper;
	
	
	
	//======================//
	// multiplayer handling //
	//======================//
	//region
	
	@Override
	public boolean hasSinglePlayerServer() { return MINECRAFT.#if MC_VER <= MC_1_12_2 isSingleplayer() #else hasSingleplayerServer() #endif; }
	@Override
	public boolean clientConnectedToDedicatedServer()
	{
		return MINECRAFT.#if MC_VER <= MC_1_12_2 getCurrentServerData() #else getCurrentServer() #endif != null
			&& !this.hasSinglePlayerServer();
	}
	@Override
	public boolean connectedToReplay()
	{
		return MINECRAFT.#if MC_VER <= MC_1_12_2 getCurrentServerData() #else getCurrentServer() #endif == null
			&& !this.hasSinglePlayerServer() ;
	}

	
	@Override
	public String getCurrentServerName() 
	{
		if (this.connectedToReplay())
		{
			return ClientOnlySaveStructure.REPLAY_SERVER_FOLDER_NAME;
		}
		else
		{
			ServerData server = MINECRAFT.#if MC_VER <= MC_1_12_2 getCurrentServerData() #else getCurrentServer() #endif;
			return (server != null) ? server.#if MC_VER <= MC_1_12_2 serverName #else name #endif : "NULL";
		}
	}
	@Override
	public String getCurrentServerIp() 
	{
		if (this.connectedToReplay())
		{
			return "";
		}
		else
		{
			ServerData server = MINECRAFT.#if MC_VER <= MC_1_12_2 getCurrentServerData() #else getCurrentServer() #endif;
			return (server != null) ? server.#if MC_VER <= MC_1_12_2 serverIP #else ip #endif : "NA";
		}
	}
	@Override
	public String getCurrentServerVersion()
	{
		ServerData server = MINECRAFT.#if MC_VER <= MC_1_12_2 getCurrentServerData() #else getCurrentServer() #endif;
		return (server != null) ? server.#if MC_VER <= MC_1_12_2 gameVersion #else version.getString() #endif : "UNKOWN";
	}
	
	//endregion
	
	
	
	//=================//
	// player handling //
	//=================//
	//region
	
	public #if MC_VER <= MC_1_12_2 EntityPlayerSP #else LocalPlayer #endif getPlayer() { return MINECRAFT.player; }
	
	@Override
	public boolean playerExists() { return MINECRAFT.player != null; }
	
	@Override
	public DhBlockPos getPlayerBlockPos()
	{
		#if MC_VER <= MC_1_12_2 EntityPlayerSP #else LocalPlayer #endif player = this.getPlayer();
		if (player == null)
		{
			return new DhBlockPos(0, 0, 0);	
		}
		
		BlockPos playerPos = player.#if MC_VER <= MC_1_12_2 getPosition() #else blockPosition() #endif;
		return new DhBlockPos(playerPos.getX(), playerPos.getY(), playerPos.getZ());
	}
	
	@Override
	public DhChunkPos getPlayerChunkPos()
	{
		#if MC_VER <= MC_1_12_2 EntityPlayerSP #else LocalPlayer #endif player = this.getPlayer();
		if (player == null)
		{
			return new DhChunkPos(0, 0);
		}
		
		#if MC_VER <= MC_1_12_2
		ChunkPos playerPos = new ChunkPos(player.getPosition());
        #elif MC_VER < MC_1_17_1
        ChunkPos playerPos = new ChunkPos(player.blockPosition());
        #else
		ChunkPos playerPos = player.chunkPosition();
        #endif
		
		#if MC_VER <= MC_1_21_11
		return new DhChunkPos(playerPos.x, playerPos.z);
		#else
		return new DhChunkPos(playerPos.x(), playerPos.z());
		#endif
	}
	
	//endregion
	
	
	
	//================//
	// level handling //
	//================//
	//region
	
	@Nullable
	@Override
	public IClientLevelWrapper getWrappedClientLevel() { return this.getWrappedClientLevel(false); }
	
	@Override
	@Nullable
	public IClientLevelWrapper getWrappedClientLevel(boolean bypassLevelKeyManager)
	{
		#if MC_VER <= MC_1_12_2 WorldClient #else ClientLevel #endif level = MINECRAFT.#if MC_VER <= MC_1_12_2 world #else level #endif;
		if (level == null)
		{
			return null;
		}
		
		return ClientLevelWrapper.getWrapper(level, bypassLevelKeyManager);
	}
	
	//endregion
	
	
	
	//===========//
	// messaging //
	//===========//
	//region
	
	@Override
	public void sendChatMessage(String string)
	{
		#if MC_VER <= MC_1_12_2 EntityPlayerSP #else LocalPlayer #endif player = this.getPlayer();
		if (player == null)
		{
			return;
		}
		
		#if MC_VER <= MC_1_12_2
		player.sendMessage(new TextComponentString(string));
        #elif MC_VER < MC_1_19_2
		player.sendMessage(new TextComponent(string), getPlayer().getUUID());
        #elif MC_VER < MC_1_21_9
		player.displayClientMessage(net.minecraft.network.chat.Component.translatable(string), /*isOverlay*/false);
		#else
		
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("MinecraftClientWrapper sendChatMessage", () -> 
		{
			#if MC_VER <= MC_1_21_11
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable(string), /*isOverlay*/false);
			#else
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(string));
			#endif
		});
        #endif
	}
	
	@Override
	public void sendOverlayMessage(String string)
	{
		#if MC_VER <= MC_1_12_2 EntityPlayerSP #else LocalPlayer #endif player = this.getPlayer();
		if (player == null)
		{
			return;
		}
		
		#if MC_VER <= MC_1_12_2
		MINECRAFT.ingameGUI.setOverlayMessage(string, /*animateColor*/false);
        #elif MC_VER < MC_1_19_2
		player.displayClientMessage(new TextComponent(string), /*isOverlay*/true);
		#elif MC_VER <= MC_1_21_11
		player.displayClientMessage(net.minecraft.network.chat.Component.translatable(string), /*isOverlay*/true);
        #else
		player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(string));
        #endif
	}
	
	//endregion
	
	
	
	//==========================//
	// vanilla option overrides //
	//==========================//
	//region
	
	public void disableVanillaClouds()
	{
		LOGGER.info("Disabling vanilla clouds... This is done to prevent vanilla clouds from rendering on top of Distant Horizons LODs.");
		
		#if MC_VER <= MC_1_12_2
		MINECRAFT.gameSettings.clouds = 0;
		#elif MC_VER <= MC_1_18_2
		MINECRAFT.options.renderClouds = CloudStatus.OFF;
		#else
		MINECRAFT.options.cloudStatus().set(CloudStatus.OFF);
		#endif
	}
	
	public void disableVanillaChunkFadeIn()
	{
		LOGGER.info("Disabling vanilla chunk fade in... This is done to prevent vanilla chunks from flashing on the Distant Horizons boarder when moving (which is distracting).");
		
		#if MC_VER <= MC_1_21_10
		// chunk fade in was added MC 1.21.11
		#else
		MINECRAFT.options.chunkSectionFadeInTime().set(0.0);
		#endif
	}
	
	public void disableFabulousTransparency()
	{
		String reasoning = "This is done to fix vanilla chunks (specifically water blocks) not fading into Distant Horizons LODs when DH's 'Vanilla Fade' option is enabled.";
		
		#if MC_VER <= MC_1_12_2
		// fabulous graphics was added in MC 1.16
		#elif MC_VER <= MC_1_18_2
		LOGGER.info("Disabling fabulous graphics... "+reasoning);
		
		GraphicsStatus oldGraphicsStatus = MINECRAFT.options.graphicsMode;
		if (oldGraphicsStatus == GraphicsStatus.FABULOUS)
		{
			MINECRAFT.options.graphicsMode = GraphicsStatus.FANCY;
		}
		#elif MC_VER <= MC_1_21_10
		LOGGER.info("Disabling fabulous graphics... "+reasoning);
		
		GraphicsStatus oldGraphicsStatus = MINECRAFT.options.graphicsMode().get();
		if (oldGraphicsStatus == GraphicsStatus.FABULOUS)
		{
			MINECRAFT.options.graphicsMode().set(GraphicsStatus.FANCY);
		}
		#else
		LOGGER.info("Disabling improved transparency... "+reasoning);
			
		MINECRAFT.options.improvedTransparency().set(false);
		#endif
	}
	
	//endregion
	
	
	
	//======//
	// misc //
	//======//
	//region
	
	/** 
	 * no override and not included in {@link IMinecraftClientWrapper}
	 * since this would only be used in common/client, not core.
	 */
	#if MC_VER > MC_1_12_2
	public 
		#if MC_VER < MC_1_21_9 long
		#else Window 
		#endif
		getGlfwWindowId()
	{
		#if MC_VER < MC_1_21_9
		long glfwWindowId = MINECRAFT.getWindow().getWindow();
		return glfwWindowId;
		#else
		return MINECRAFT.getWindow();
		#endif
	}
	#endif
	
	@Override
	public IProfilerWrapper getProfiler()
	{
		#if MC_VER <= MC_1_12_2 Profiler #else ProfilerFiller #endif profiler;
		#if MC_VER <= MC_1_12_2
		profiler = MINECRAFT.profiler;
		#elif MC_VER < MC_1_21_3
		profiler = MINECRAFT.getProfiler();
		#else
		profiler = Profiler.get();
		#endif
		
		if (this.profilerWrapper == null)
		{
			this.profilerWrapper = new ProfilerWrapper(profiler);
		}
		else if (profiler != this.profilerWrapper.profiler)
		{
			this.profilerWrapper.profiler = profiler;
		}
		
		return this.profilerWrapper;
	}
	
	@Override
	public void crashMinecraft(String errorMessage, Throwable exception)
	{
		LOGGER.fatal(ModInfo.READABLE_NAME + " had the following error: [" + errorMessage + "]. Crashing Minecraft...", exception);
		
		// Only crash once the renderer has been set up.
		// If the renderer hasn't been set up yet crashing MC will
		// cause a Blaze3D/UI error instead of the error we're trying to send.
		executeOnRenderThread(() -> 
		{
			CrashReport report = new CrashReport(errorMessage, exception);
			#if MC_VER <= MC_1_12_2
			MINECRAFT.crashed(report);
			#elif MC_VER < MC_1_20_4
			Minecraft.crash(report);
			#else
			MINECRAFT.delayCrash(report);
			#endif
		});
	}
	
	@Override
	public void executeOnRenderThread(Runnable runnable) { MINECRAFT.#if MC_VER <= MC_1_12_2 addScheduledTask #else execute #endif(runnable); }
	
	@Override
	public void showDialog(String title, String message, String dialogType, String iconType)
	{ NativeDialogUtil.showDialog(title, message, dialogType, iconType); }
	
	//endregion
	
	
	
	//=============//
	// mod support //
	//=============//
	//region
	
	@Override
	public Object getOptionsObject() { return MINECRAFT.#if MC_VER <= MC_1_12_2 gameSettings #else options #endif; }
	
	//endregion
	
	
	
	//========//
	// shared //
	//========//
	//region
	
	@Override
	public boolean isDedicatedServer() { return false; }
	
	@Override
	public File getInstallationDirectory() { return MINECRAFT.#if MC_VER <= MC_1_12_2 gameDir #else gameDirectory #endif; }
	
	@Override
	public int getPlayerCount()
	{
		// can be null if the server hasn't finished booting up yet
		if (MINECRAFT.#if MC_VER <= MC_1_12_2 getIntegratedServer() #else getSingleplayerServer() #endif == null)
		{
			return 1;
		}
		else
		{
			#if MC_VER <= MC_1_12_2
			return MINECRAFT.getIntegratedServer().getCurrentPlayerCount();
			#else
			return MINECRAFT.getSingleplayerServer().getPlayerCount();
			#endif
		}
	}
	
	//endregion
	
	
	
}
