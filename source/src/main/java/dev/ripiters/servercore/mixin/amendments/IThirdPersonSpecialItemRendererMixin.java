package dev.ripiters.servercore.mixin.amendments;

import dev.ripiters.servercore.client.compat.AmendmentsPlayerAnimationsCompat;
import net.mehvahdjukaar.moonlight.api.item.IThirdPersonSpecialItemRenderer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = IThirdPersonSpecialItemRenderer.class, remap = false)
public interface IThirdPersonSpecialItemRendererMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true, remap = false)
    private static void servercore$disableLanternThirdPersonRenderer(Item target, CallbackInfoReturnable<IThirdPersonSpecialItemRenderer> cir) {
        if (!AmendmentsPlayerAnimationsCompat.isActive()) {
            return;
        }

        /*
         * For lanterns:
         * <p>
         * DO NOT return Amendments' LanternRendererExtension.
         * <p>
         * Returning null makes Moonlight fall back to the normal
         * third-person item rendering path.
         * <p>
         * That path is important because EMF / Fresh Animations can
         * then apply the RP's right_item / left_item transforms.
         */
        if (AmendmentsPlayerAnimationsCompat.isLantern(target.getDefaultInstance())) {
            cir.setReturnValue(null);
        }
    }
}