package dev.ripiters.servercore.mixin.copperagebackport;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.amendments.common.WallLanternServerResources")
public abstract class WallLanternResourcesMixin {

    @Inject(method = "gatherSupportedNamespaces", at = @At("RETURN"), cancellable = true, remap = false)
    private void registerCopperAgeNamespace(CallbackInfoReturnable<Collection<String>> cir) {
        List<String> namespaces = new ArrayList<>(cir.getReturnValue());
        namespaces.add("copperagebackport");
        cir.setReturnValue(namespaces);
    }
}