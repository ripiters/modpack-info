package dev.ripiters.servercore;

import com.mojang.logging.LogUtils;
import dev.ripiters.servercore.commands.ServerCoreCommands;
import dev.ripiters.servercore.config.ServerCoreConfig;
import dev.ripiters.servercore.handlers.minecraft.CreateTrainHandler;
import dev.ripiters.servercore.handlers.minecraft.DungeonSpawnExclusionHandler;
import dev.ripiters.servercore.handlers.minecraft.RecipeRemoveHandler;
import dev.ripiters.servercore.handlers.minecraft.TagModificationHandler;
import dev.ripiters.servercore.handlers.server.JoinMessageHandler;
import dev.ripiters.servercore.handlers.server.SpectateHandler;
import dev.ripiters.servercore.logging.LogFilter;
import dev.ripiters.servercore.network.ServerPayloadHandler;
import dev.ripiters.servercore.network.packet.XrayDetectedPayload;
import dev.ripiters.servercore.util.VersionChecker;
import dev.ripiters.servercore.worldgen.ModFeatures;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(ServerCore.MODID)
public class ServerCore {
    public static final String MODID = "servercore";
    public static final String NAME = "ServerCore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ServerCore(IEventBus modEventBus, ModContainer modContainer) {
        ModLoadingContext.get();

        ServerCoreConfig.register(modContainer);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::registerNetwork);

        ModFeatures.register(modEventBus);

        NeoForge.EVENT_BUS.register(TagModificationHandler.class);
        NeoForge.EVENT_BUS.register(RecipeRemoveHandler.class);
        NeoForge.EVENT_BUS.register(SpectateHandler.class);
        NeoForge.EVENT_BUS.register(CreateTrainHandler.class);
        NeoForge.EVENT_BUS.register(DungeonSpawnExclusionHandler.class);
        NeoForge.EVENT_BUS.register(JoinMessageHandler.class);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LogFilter.init();

        ServerCore.LOGGER.debug(ServerCore.NAME + " Registration complete.");
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        ServerCoreConfig.onLoad(event);
        VersionChecker.checkVersionAsync();
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        ServerCoreConfig.onReload(event);
        VersionChecker.checkVersionAsync();
    }

    private void registerNetwork(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                XrayDetectedPayload.TYPE,
                XrayDetectedPayload.STREAM_CODEC,
                ServerPayloadHandler::handleXrayDetection
        );
    }

    public void onRegisterCommands(RegisterCommandsEvent event) {
        ServerCoreCommands.register(event.getDispatcher());
    }
}