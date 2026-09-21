package dev.ripiters.servercore.mixin.amendments;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ripiters.servercore.client.compat.AmendmentsPlayerAnimationsCompat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.amendments.client.renderers.LanternRendererExtension", remap = false)
public abstract class LanternRendererExtensionMixin {

    @Inject(method = "poseRightArm", at = @At("HEAD"), cancellable = true, remap = false)
    private <T extends LivingEntity> void servercore$disableRightArmPose(ItemStack stack, HumanoidModel<T> model, T entity, HumanoidArm arm, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player && AmendmentsPlayerAnimationsCompat.isActive() && AmendmentsPlayerAnimationsCompat.isLantern(stack)) {

            /*
             * IMPORTANT:
             * <p>
             * Amendments must NOT apply its own lantern arm animation
             * in third person.
             * <p>
             * false = do not skip the normal/default animation.
             * <p>
             * This allows EMF / Fresh Animations to control the arm.
             */
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "poseLeftArm", at = @At("HEAD"), cancellable = true, remap = false)
    private <T extends LivingEntity> void servercore$disableLeftArmPose(ItemStack stack, HumanoidModel<T> model, T entity, HumanoidArm arm, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player && AmendmentsPlayerAnimationsCompat.isActive() && AmendmentsPlayerAnimationsCompat.isLantern(stack)) {

            /*
             * Let EMF / Fresh Animations handle the arm.
             */
            cir.setReturnValue(false);
        }
    }

    /*
     * FIRST PERSON ONLY
     * <p>
     * Amendments' first-person lantern renderer explicitly calls
     * ItemInHandRenderer.renderPlayerArm().
     * <p>
     * We intercept that call and pause EMF only while this one arm
     * is being rendered, so the lantern does not first use the EMF
     * first-person animation and then snap to Amendments' pose.
     */
    @Redirect(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V"), remap = false)
    private void servercore$renderLanternArmWithoutEmfAnimation(ItemInHandRenderer renderer, PoseStack poseStack, MultiBufferSource buffer, int light, float equipProgress, float swingProgress, HumanoidArm arm) {
        AmendmentsPlayerAnimationsCompat.renderPlayerArmWithoutEmfLightAnimation(renderer, poseStack, buffer, light, equipProgress, swingProgress, arm);
    }
}