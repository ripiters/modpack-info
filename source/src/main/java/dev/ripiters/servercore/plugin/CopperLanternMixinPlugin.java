package dev.ripiters.servercore.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.neoforged.fml.loading.FMLLoader;

import java.util.List;
import java.util.Set;

public class CopperLanternMixinPlugin implements IMixinConfigPlugin {

    private boolean applyCopperAgeMixins;

    @Override
    public void onLoad(String mixinPackage) {
        boolean hasAmendments = FMLLoader.getLoadingModList().getModFileById("amendments") != null;
        boolean hasCopperAge = FMLLoader.getLoadingModList().getModFileById("copperagebackport") != null;
        this.applyCopperAgeMixins = hasAmendments && hasCopperAge;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("copperagebackport")) {
            return applyCopperAgeMixins;
        }
        return true;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}