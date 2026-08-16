# Shared source layers

Tellus version projects are compatibility overlays, not independent copies of the mod.
Every implementation should live in the narrowest layer that can compile it:

- `src/`: supported by every Minecraft version and loader.
- `shared/pre-26/`: shared by Minecraft 1.20.1 and 1.21.1.
- `shared/post-1201/`: shared by Minecraft 1.21.1 and 26.2.
- `shared/mc1201/`, `shared/mc1211/`, `shared/mc262/`: shared by all loaders for one Minecraft version.
- `shared/fabric/`, `shared/neoforge/`, `shared/forge-family/`: loader-family APIs shared across Minecraft versions.
- `shared/pre-26-fabric/`, `shared/pre-26-forge/`: APIs shared by the two pre-26 targets in that loader family.

The module-local `mc*/src` trees should contain only code or resources that cannot be
expressed by one of these layers. `./gradlew checkVersionSourceDuplication` prevents
byte-identical Java implementations from being copied back into multiple layers.

When Minecraft or loader APIs differ, keep the behavior in a shared implementation and
put only the changed signatures in a small compatibility class. The main examples are
`MinecraftVersionCompat`, `ClientMinecraftCompat`, and the version-specific chunk-generator
bridges. Loader lifecycle registration follows the same rule through `TellusRuntimePlatform`;
the six `Tellus` entrypoints should remain thin wrappers around the universal `TellusCommon`.
