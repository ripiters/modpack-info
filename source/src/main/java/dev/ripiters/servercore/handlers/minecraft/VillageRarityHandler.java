package dev.ripiters.servercore.handlers.minecraft;

import dev.ripiters.servercore.ServerCore;
import dev.ripiters.servercore.config.ServerCoreConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.lang.reflect.Field;

@EventBusSubscriber(modid = ServerCore.MODID)
public class VillageRarityHandler {

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        int villageSpacing = ServerCoreConfig.COMMON.getVillageSpacingValue();
        int villageSeparation = ServerCoreConfig.COMMON.getVillageSeparationValue();

        if (villageSpacing <= villageSeparation) {
            ServerCore.LOGGER.warn("Village spacing must be greater than separation! Skipping village rarity modification.");
            return;
        }

        Registry<StructureSet> structureSetRegistry = event.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        StructureSet villageSet = structureSetRegistry.get(ResourceLocation.withDefaultNamespace("villages"));

        if (villageSet != null && villageSet.placement() instanceof RandomSpreadStructurePlacement placement) {
            try {
                Field spacingField = RandomSpreadStructurePlacement.class.getDeclaredField("spacing");
                spacingField.setAccessible(true);
                spacingField.setInt(placement, villageSpacing);

                Field separationField = RandomSpreadStructurePlacement.class.getDeclaredField("separation");
                separationField.setAccessible(true);
                separationField.setInt(placement, villageSeparation);

                ServerCore.LOGGER.info("Applied custom village generation spacing ({}) and separation ({}).", villageSpacing, villageSeparation);
            } catch (Exception e) {
                ServerCore.LOGGER.error("Failed to modify village placement via reflection: {}", e.getMessage());
            }
        }
    }
}