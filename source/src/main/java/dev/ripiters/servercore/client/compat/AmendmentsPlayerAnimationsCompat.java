package dev.ripiters.servercore.client.compat;

import net.mehvahdjukaar.amendments.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.neoforged.fml.ModList;
import com.mojang.blaze3d.vertex.PoseStack;

import java.lang.reflect.Method;

public final class AmendmentsPlayerAnimationsCompat {

    private static final String AMENDMENTS_MOD_ID = "amendments";
    private static final ResourceLocation PA_PLAYER_MODEL = ResourceLocation.withDefaultNamespace("emf/cem/player.jem");
    private static final ResourceLocation PA_FIRST_PERSON_ANIMS = ResourceLocation.withDefaultNamespace("emf/cem/a_player_firstperson.jpm");

    private static volatile boolean emfApiInitialized;
    private static Method emfEntityOf;
    private static Method pauseAllCustomAnimations;
    private static Method resumeAllCustomAnimations;

    private AmendmentsPlayerAnimationsCompat() {
    }

    /**
     * Returns true only when Amendments is loaded and the Player Animations /
     * EMF resource pack is available through the active resource manager.
     */
    public static boolean isActive() {
        if (!ModList.get().isLoaded(AMENDMENTS_MOD_ID)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null || minecraft.getResourceManager() == null) {
            return false;
        }

        return minecraft.getResourceManager().getResource(PA_PLAYER_MODEL).isPresent() || minecraft.getResourceManager().getResource(PA_FIRST_PERSON_ANIMS).isPresent();
    }

    public static boolean isTorch(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Block block = Block.byItem(stack.getItem());

        return block instanceof TorchBlock;
    }

    public static boolean isLantern(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Block block = Block.byItem(stack.getItem());

        return block instanceof LanternBlock;
    }

    public static boolean isTorchOrLantern(ItemStack stack) {
        return isTorch(stack) || isLantern(stack);
    }

    /**
     * Applies the exact Amendments torch / lantern holding pose to one arm.
     * <p>
     * Only X rotation is replaced, so Y/Z rotations can remain controlled
     * by Player Animations where applicable.
     */
    public static void applyAmendmentsLightPose(Player player, HumanoidModel<?> model, HumanoidArm arm) {
        if (!isActive() || player == null || model == null) {
            return;
        }

        ItemStack stack = getStackForArm(player, arm);

        if (isLantern(stack)) {
            applyLanternPose(model, arm);
        } else if (isTorch(stack)) {
            applyTorchPose(model, arm);
        }
    }

    /**
     * Called by the Amendments first-person lantern renderer.
     * <p>
     * The important part is that EMF custom animations are paused for the
     * duration of renderPlayerArm(), so EMF cannot animate the arm and then
     * visibly snap it to Amendments' pose.
     */
    public static void renderPlayerArmWithoutEmfLightAnimation(ItemInHandRenderer renderer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float equipProgress, float swingProgress, HumanoidArm arm) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!isActive() || minecraft == null || minecraft.player == null || renderer == null) {
            renderer.renderPlayerArm(poseStack, buffer, packedLight, equipProgress, swingProgress, arm);
            return;
        }

        AbstractClientPlayer player = minecraft.player;
        PlayerRenderer playerRenderer = getPlayerRenderer(player);

        if (playerRenderer == null) {
            renderer.renderPlayerArm(poseStack, buffer, packedLight, equipProgress, swingProgress, arm
            );

            return;
        }

        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        boolean emfPaused = pauseEmfAnimations(player);

        try {
            /*
             * EMF is paused FIRST.
             * <p>
             * This is important because otherwise EMF can immediately
             * overwrite the Amendments rotation again.
             */
            applyAmendmentsLightPose(player, model, arm);
            renderer.renderPlayerArm(poseStack, buffer, packedLight, equipProgress, swingProgress, arm);

        } finally {
            if (emfPaused) {
                resumeEmfAnimations(player);
            }
        }
    }

    private static PlayerRenderer getPlayerRenderer(AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null) {
            return null;
        }

        Object renderer = minecraft.getEntityRenderDispatcher().getRenderer(player);

        if (renderer instanceof PlayerRenderer playerRenderer) {
            return playerRenderer;
        }

        return null;
    }

    private static void applyTorchPose(HumanoidModel<?> model, HumanoidArm arm) {
        float xRot;

        if (ClientConfigs.HOLDING_ANIMATION_FIXED.get()) {
            xRot = -1.3F;
        } else {
            xRot = Mth.clamp(wrapRad(-1.4F + model.head.xRot), -2.4F, -0.2F);
        }

        setArmXRot(model, arm, xRot);
    }

    private static void applyLanternPose(HumanoidModel<?> model, HumanoidArm arm) {
        boolean up = ClientConfigs.LANTERN_HOLDING_UP.get();
        float xRot;
        if (ClientConfigs.HOLDING_ANIMATION_FIXED.get()) {
            if (arm == HumanoidArm.RIGHT) {
                xRot = up ? -1.9F : -1.0F;
            } else {
                xRot = up ? -1.9F : 1.0F;
            }
        } else {
            float v = up ? -1.9F : -1.2F;
            xRot = Mth.clamp(wrapRad(v + model.head.xRot), -2.4F, -0.5F);
        }

        setArmXRot(model, arm, xRot);
    }

    private static void setArmXRot(HumanoidModel<?> model, HumanoidArm arm, float xRot) {
        if (arm == HumanoidArm.RIGHT) {
            model.rightArm.xRot = xRot;

            if (model instanceof PlayerModel<?> playerModel) {
                playerModel.rightSleeve.xRot = xRot;
            }

        } else {
            model.leftArm.xRot = xRot;

            if (model instanceof PlayerModel<?> playerModel) {
                playerModel.leftSleeve.xRot = xRot;
            }
        }
    }

    private static ItemStack getStackForArm(Player player, HumanoidArm arm) {
        if (arm == player.getMainArm()) {
            return player.getMainHandItem();
        }

        return player.getOffhandItem();
    }

    /**
     * Uses EMF's public animation API through reflection so ServerCore does
     * not have a hard compile-time dependency on EMF.
     */
    private static boolean pauseEmfAnimations(Player player) {
        if (!initializeEmfApi()) {
            return false;
        }

        try {
            Object emfEntity = emfEntityOf.invoke(null, player);
            Object result = pauseAllCustomAnimations.invoke(null, emfEntity);
            return Boolean.TRUE.equals(result);

        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void resumeEmfAnimations(Player player) {
        if (!initializeEmfApi()) {
            return;
        }

        try {
            Object emfEntity = emfEntityOf.invoke(null, player);
            resumeAllCustomAnimations.invoke(null, emfEntity);

        } catch (Throwable ignored) { }
    }

    private static boolean initializeEmfApi() {
        if (emfApiInitialized) {
            return emfEntityOf != null && pauseAllCustomAnimations != null && resumeAllCustomAnimations != null;
        }

        synchronized (AmendmentsPlayerAnimationsCompat.class) {
            if (emfApiInitialized) {
                return emfEntityOf != null && pauseAllCustomAnimations != null && resumeAllCustomAnimations != null;
            }

            try {
                Class<?> apiClass = Class.forName("traben.entity_model_features.EMFAnimationApi");
                Class<?> emfEntityClass = Class.forName("traben.entity_model_features.utils.EMFEntity");

                emfEntityOf = apiClass.getMethod("emfEntityOf", net.minecraft.world.entity.Entity.class);
                pauseAllCustomAnimations = apiClass.getMethod("pauseAllCustomAnimationsForEntity", emfEntityClass);
                resumeAllCustomAnimations = apiClass.getMethod("resumeAllCustomAnimationsForEntity", emfEntityClass);

            } catch (Throwable ignored) {
                emfEntityOf = null;
                pauseAllCustomAnimations = null;
                resumeAllCustomAnimations = null;
            }

            emfApiInitialized = true;

            return emfEntityOf != null && pauseAllCustomAnimations != null && resumeAllCustomAnimations != null;
        }
    }

    private static float wrapRad(float value) {
        value %= Mth.TWO_PI;

        if (value >= Mth.PI) {
            value -= Mth.TWO_PI;
        }

        if (value < -Mth.PI) {
            value += Mth.TWO_PI;
        }

        return value;
    }
}