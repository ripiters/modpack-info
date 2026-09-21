package dev.ripiters.servercore.mixin;

import dev.ripiters.servercore.tag.ModTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleContents.Mutable.class)
public class BundleContentsMixin {

    @Inject(method = "tryInsert", at = @At("HEAD"), cancellable = true, remap = false)
    private void servercore$preventTaggedItemsFromBundle(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.is(ModTags.Items.UNBUNDLABLE)) {
            cir.setReturnValue(0);
        }
    }
}