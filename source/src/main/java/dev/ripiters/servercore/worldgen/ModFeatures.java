package dev.ripiters.servercore.worldgen;

import dev.ripiters.servercore.ServerCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * LOL
 */
public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, ServerCore.MODID);
    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}