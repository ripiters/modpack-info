package dev.ripiters.servercore.pack;

import dev.ripiters.servercore.ServerCore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.nio.file.Path;
import java.util.Optional;

@EventBusSubscriber(modid = ServerCore.MODID)
public final class ModDatapackEvents {

    private ModDatapackEvents() { }

    @SubscribeEvent
    public static void registerBuiltInDatapack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        if (ModList.get().isLoaded("epic")) {
            registerPack(event, "servercore_epic_structures", "ServerCore Epic Structures Configuration", "Epic_Structures_Server");
        }
    }

    private static void registerPack(AddPackFindersEvent event, String id, String title, String resourcePath) {
        Path packPath = ModList.get().getModFileById(ServerCore.MODID).getFile().findResource("pack/" + resourcePath);
        ServerCore.LOGGER.info("Registering built-in datapack '{}' from {}", id, packPath);
        PackLocationInfo packInfo = new PackLocationInfo(id, Component.literal(title), PackSource.BUILT_IN, Optional.empty());
        PackSelectionConfig selectionConfig = new PackSelectionConfig(true, Pack.Position.TOP, false);
        Pack.ResourcesSupplier resourcesSupplier = new PathPackResources.PathResourcesSupplier(packPath);
        Pack pack = Pack.readMetaAndCreate(packInfo, resourcesSupplier, PackType.SERVER_DATA, selectionConfig);

        if (pack == null) {
            ServerCore.LOGGER.error("Failed to create built-in datapack '{}'. " + "Check pack.mcmeta and directory structure: {}", id, packPath);
            return;
        }

        event.addRepositorySource(repository -> repository.accept(pack));
        ServerCore.LOGGER.info("Built-in datapack '{}' registered successfully.", id);
    }
}