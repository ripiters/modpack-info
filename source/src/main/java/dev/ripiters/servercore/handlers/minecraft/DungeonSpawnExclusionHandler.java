package dev.ripiters.servercore.handlers.minecraft;

import dev.ripiters.servercore.ServerCore;
import dev.ripiters.servercore.config.ServerCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.lang.reflect.Field;
import java.util.Map;

@EventBusSubscriber(modid = ServerCore.MODID)
public class DungeonSpawnExclusionHandler {

    // Checks whether coordinates are inside the spawn exclusion zone
    public static boolean isWithinExclusionZone(BlockPos pos) {
        if (!ServerCoreConfig.COMMON.preventSpawnDungeons.get()) {
            return false;
        }
        int radius = ServerCoreConfig.COMMON.spawnExclusionRadius.get();
        return Math.abs(pos.getX()) <= radius && Math.abs(pos.getZ()) <= radius;
    }

    // Validates configured dungeon spacing against separation values
    public static boolean isDungeonSpacingConfigValid() {
        int spacing = ServerCoreConfig.COMMON.getLargeDungeonSpacingValue();
        int separation = ServerCoreConfig.COMMON.getLargeDungeonSeparationValue();
        return spacing > separation;
    }

    // Applied dynamically when the world loads
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (!isDungeonSpacingConfigValid()) {
            ServerCore.LOGGER.warn("Dungeon spacing must be greater than separation! Check servercore-common.toml.");
            return;
        }

        int spacing = ServerCoreConfig.COMMON.getLargeDungeonSpacingValue();
        int separation = ServerCoreConfig.COMMON.getLargeDungeonSeparationValue();

        Registry<StructureSet> structureSetRegistry = event.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);

        for (Map.Entry<ResourceKey<StructureSet>, StructureSet> entry : structureSetRegistry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            String path = id.getPath();
            String namespace = id.getNamespace();

            boolean isEpicStructure = namespace.equals("epic") || namespace.equals("mr_epic_structuresdungeons");
            boolean isDungeonOrObelisk = path.contains("dungeon") || path.contains("obelisk");

            if (isEpicStructure || isDungeonOrObelisk) {
                StructureSet structureSet = entry.getValue();
                StructurePlacement placement = structureSet.placement();

                if (placement instanceof RandomSpreadStructurePlacement oldPlacement) {
                    try {
                        Field spacingField = RandomSpreadStructurePlacement.class.getDeclaredField("spacing");
                        Field separationField = RandomSpreadStructurePlacement.class.getDeclaredField("separation");

                        spacingField.setAccessible(true);
                        separationField.setAccessible(true);

                        spacingField.setInt(oldPlacement, spacing);
                        separationField.setInt(oldPlacement, separation);

                        ServerCore.LOGGER.info("Applied custom structure spacing ({}) and separation ({}) for structure set [{}]", spacing, separation, id);
                    } catch (Exception e) {
                        ServerCore.LOGGER.error("Failed to modify structure placement for [{}]: {}", id, e.getMessage());
                    }
                }
            }
        }
    }

    @EventBusSubscriber(modid = ServerCore.MODID)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void onConfigReload(ModConfigEvent.Reloading event) {
            if (event.getConfig().getSpec() == ServerCoreConfig.COMMON.specification) {
                if (isDungeonSpacingConfigValid()) {
                    ServerCore.LOGGER.info("ServerCore dungeon/obelisk configuration reloaded successfully.");
                } else {
                    ServerCore.LOGGER.error("Invalid dungeon spacing/separation settings detected during reload!");
                }
            }
        }
    }
}