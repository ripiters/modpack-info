package dev.ripiters.servercore.mixin.amendments;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ripiters.servercore.client.compat.AmendmentsPlayerAnimationsCompat;
import net.mehvahdjukaar.amendments.configs.ClientConfigs;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.amendments.client.renderers.TorchRendererExtension", remap = false)
public abstract class TorchRendererExtensionMixin {

    @Inject(method = "poseRightArm", at = @At("HEAD"), cancellable = true, remap = false)
    private <T extends LivingEntity> void servercore$disableRightArmPose(ItemStack stack, HumanoidModel<T> model, T entity, HumanoidArm arm, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player && AmendmentsPlayerAnimationsCompat.isActive() && AmendmentsPlayerAnimationsCompat.isTorch(stack)) {

            /*
             * Player Animations / EMF is responsible for the third-person
             * arm pose. Amendments should not overwrite it.
             */
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "poseLeftArm", at = @At("HEAD"), cancellable = true, remap = false)
    private <T extends LivingEntity> void servercore$disableLeftArmPose(ItemStack stack, HumanoidModel<T> model, T entity, HumanoidArm arm, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player && AmendmentsPlayerAnimationsCompat.isActive() && AmendmentsPlayerAnimationsCompat.isTorch(stack)) {
            cir.setReturnValue(false);
        }
    }

    /*
     * Amendments:
     * <p>
     * private static void renderTorchModel(...)
     * <p>
     * Static invokers cannot be abstract in Java, therefore the normal
     * Mixin invoker stub is used here.
     */
    @Invoker("renderTorchModel")
    private static void servercore$renderTorchModel(LivingEntity entity, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, boolean left) {
        throw new AssertionError();
    }

    /*
     * Amendments:
     * <p>
     * private void renderFlame(...)
     * <p>
     * This is an instance method, therefore the invoker MUST be abstract.
     */
    @Invoker("renderFlame")
    protected abstract void servercore$renderFlame(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, ItemStack stack);

    @Inject(method = "renderThirdPersonItem", at = @At("HEAD"), cancellable = true, remap = false)
    private <T extends Player, M extends EntityModel<T> & ArmedModel & HeadedModel>
    void servercore$renderCompatibleThirdPersonTorch(M parentModel, LivingEntity entity, ItemStack stack, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (!(entity instanceof Player) || !AmendmentsPlayerAnimationsCompat.isActive() || !AmendmentsPlayerAnimationsCompat.isTorch(stack)) {
            return;
        }

        boolean left = humanoidArm == HumanoidArm.LEFT;
        poseStack.pushPose();

        /*
         * Amendments' original renderer:
         * <p>
         * translateToHand()
         * rotate X -90°
         * rotate Y 180°
         * translate
         * renderTorchModel()
         * renderFlame()
         * <p>
         * Keep all item transforms but DON'T add another
         * rotation based on arm.xRot / arm.zRot.
         * <p>
         * Player Animations' arm transform is already present in
         * translateToHand().
         */
        parentModel.translateToHand(humanoidArm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate((left ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
        poseStack.scale(1.0F, 1.0F, 1.0F);
        poseStack.translate(0.0F, 3.0F / 16.0F, 0.125F);

        /*
         * Render the actual Amendments torch.
         */
        servercore$renderTorchModel(entity, stack, poseStack, bufferSource, light, left);

        /*
         * Preserve Amendments' flame rendering.
         * <p>
         * Important: use the exact same conditions as Amendments:
         * the flame is disabled while the entity is in water.
         */
        if (ClientConfigs.TORCH_HOLDING_FLAME.get() && !entity.isInWater()) {
            servercore$renderFlame(entity, poseStack, bufferSource, stack);
        }

        poseStack.popPose();

        /*
         * Rendered the complete Amendments torch ourselves,
         * the original method must not render it a second time.
         */
        ci.cancel();
    }
}