package dev.ripiters.servercore.mixin.farmersdelight;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity", remap = false)
public class CookingPotBucketFixMixin {

    @Inject(method = "ejectIngredientRemainder", at = @At("HEAD"), cancellable = true)
    private void servercore$keepBucketInContainerSlot(ItemStack remainderStack, CallbackInfo ci) {
        if (remainderStack.is(Items.BUCKET)) {
            Object thisObject = this;
            if (thisObject instanceof CookingPotBlockEntity pot) {
                int containerSlot = CookingPotBlockEntity.CONTAINER_SLOT;
                ItemStack containerStack = pot.getInventory().getStackInSlot(containerSlot);

                if (containerStack.isEmpty()) {
                    pot.getInventory().setStackInSlot(containerSlot, remainderStack.copy());
                    ci.cancel();
                } else if (ItemStack.isSameItemSameComponents(containerStack, remainderStack) && containerStack.getCount() + remainderStack.getCount() <= containerStack.getMaxStackSize()) {
                    containerStack.grow(remainderStack.getCount());
                    ci.cancel();
                }
            }
        }
    }
}