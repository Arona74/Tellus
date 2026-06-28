package com.yucareux.tellus.platform;

import java.nio.file.Path;
import java.util.Objects;
import java.util.ServiceLoader;

public final class TellusPlatform {
   private static final TellusPlatformService SERVICE = loadService();

   private TellusPlatform() {
   }

   public static Path gameDir() {
      return SERVICE.gameDir();
   }

   public static Path configDir() {
      return SERVICE.configDir();
   }

   public static boolean isModLoaded(String modId) {
      return SERVICE.isModLoaded(Objects.requireNonNull(modId, "modId"));
   }

   private static TellusPlatformService loadService() {
      return ServiceLoader.load(TellusPlatformService.class)
         .findFirst()
         .orElseThrow(() -> new IllegalStateException("No Tellus platform service was registered"));
   }
}
