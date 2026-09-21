package dev.ripiters.servercore.config;

import dev.ripiters.servercore.ServerCore;
import net.createmod.catnip.config.ConfigBase;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@EventBusSubscriber(modid = ServerCore.MODID)
public class ServerCoreConfig {

    private static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);

    public static final CommonConfig COMMON = register(CommonConfig::new, ModConfig.Type.COMMON);

    private static <T extends ConfigBase> T register(Supplier<T> factory, ModConfig.Type side) {
        Pair<T, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    public static void register(ModContainer container) {
        if (CONFIGS.containsKey(ModConfig.Type.COMMON)) {
            container.registerConfig(ModConfig.Type.COMMON, COMMON.specification, "servercore-common.toml");
            if (FMLEnvironment.dist.isClient()) {
                ClientHandler.registerConfigScreen(container);
            }
        }
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        for (ConfigBase config : CONFIGS.values()) {
            if (config.specification == event.getConfig().getSpec()) {
                config.onLoad();
            }
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        for (ConfigBase config : CONFIGS.values()) {
            if (config.specification == event.getConfig().getSpec()) {
                config.onReload();
            }
        }
    }

    private static class ClientHandler {
        private static void registerConfigScreen(ModContainer container) {
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}