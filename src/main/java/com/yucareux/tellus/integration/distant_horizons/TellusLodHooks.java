package com.yucareux.tellus.integration.distant_horizons;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Extension points of Tellus's direct Distant Horizons LOD generation (see {@link TellusLodGenerator}). */
public final class TellusLodHooks {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static volatile TellusLodSurfaceDecorator surfaceDecorator;

   private TellusLodHooks() {
   }

   /** Sets the decorator for bare ground in direct LOD columns; {@code null} removes it. One at a time. */
   public static void registerSurfaceDecorator(TellusLodSurfaceDecorator decorator) {
      if (decorator != null && surfaceDecorator != null && surfaceDecorator != decorator) {
         LOGGER.warn("Replacing the Tellus LOD surface decorator {} with {}", surfaceDecorator, decorator);
      }
      surfaceDecorator = decorator;
      if (decorator != null) {
         LOGGER.info("Tellus LOD surface decorator registered: {}", decorator);
      }
   }

   static TellusLodSurfaceDecorator surfaceDecorator() {
      return surfaceDecorator;
   }
}
