package com.yucareux.tellus;

import com.yucareux.tellus.client.screen.EarthTeleportScreen;
import com.yucareux.tellus.network.GeoTpOpenMapPayload;
import com.yucareux.tellus.network.TellusWeatherPayload;
import com.yucareux.tellus.world.realtime.SnowGrid;
import com.yucareux.tellus.world.realtime.TemperatureGrid;
import com.yucareux.tellus.world.realtime.TellusRealtimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TellusClient {
   private TellusClient() {
   }

   public static void register(IEventBus modEventBus) {
      modEventBus.addListener(TellusClient::registerClientPayloadHandlers);
      NeoForge.EVENT_BUS.addListener(TellusClient::onClientDisconnect);
   }

   private static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
      event.register(GeoTpOpenMapPayload.TYPE, TellusClient::handleOpenMapPayload);
      event.register(TellusWeatherPayload.TYPE, TellusClient::handleWeatherPayload);
   }

   private static void handleOpenMapPayload(GeoTpOpenMapPayload payload, IPayloadContext context) {
      Minecraft.getInstance().execute(() -> {
         Minecraft minecraft = Minecraft.getInstance();
         Screen parent = minecraft.gui.screen();
         minecraft.gui.setScreen(new EarthTeleportScreen(parent, payload.latitude(), payload.longitude()));
      });
   }

   private static void handleWeatherPayload(TellusWeatherPayload payload, IPayloadContext context) {
      Minecraft.getInstance().execute(() -> {
         SnowGrid grid = payload.historicalSnowEnabled() && payload.spacingBlocks() > 0
            ? new SnowGrid(payload.centerX(), payload.centerZ(), payload.spacingBlocks(), payload.snowIndex())
            : SnowGrid.empty();
         TemperatureGrid temperatureGrid = payload.spacingBlocks() > 0
            ? new TemperatureGrid(
               payload.centerX(),
               payload.centerZ(),
               payload.spacingBlocks(),
               payload.temperatureC(),
               System.currentTimeMillis() - Math.max(0L, payload.temperatureAgeMs())
            )
            : TemperatureGrid.empty();
         TellusRealtimeState.updateWeatherState(
            payload.weatherEnabled(), payload.precipitationMode(), payload.historicalSnowEnabled(), grid, temperatureGrid
         );
      });
   }

   private static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
      TellusRealtimeState.reset();
   }
}
