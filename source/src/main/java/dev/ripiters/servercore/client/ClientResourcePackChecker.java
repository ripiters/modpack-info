package dev.ripiters.servercore.client;

import dev.ripiters.servercore.ServerCore;
import dev.ripiters.servercore.network.packet.XrayDetectedPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

@EventBusSubscriber(modid = ServerCore.MODID, value = Dist.CLIENT)
public class ClientResourcePackChecker {

    private static boolean isCheatDetected = false;
    private static String detectedReason = "";
    private static String cachedModHash = "";
    private static boolean hasReported = false;

    // Critical block model locations modified by X-Ray packs
    private static final List<ResourceLocation> SUSPICIOUS_MODELS = List.of(
            ResourceLocation.withDefaultNamespace("models/block/stone.json"),
            ResourceLocation.withDefaultNamespace("models/block/deepslate.json"),
            ResourceLocation.withDefaultNamespace("models/block/netherrack.json")
    );

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        hasReported = false;
        runDetection(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            hasReported = false;
            return;
        }

        runDetection(!hasReported);
    }

    private static void runDetection(boolean sendToServer) {
        Minecraft mc = Minecraft.getInstance();

        if (isMeteorPresent()) {
            setDetected("Meteor Client", sendToServer, mc);
            return;
        }

        if (mc.getResourcePackRepository() != null) {
            List<Pack> selectedPacks = (List<Pack>) mc.getResourcePackRepository().getSelectedPacks();

            for (Pack pack : selectedPacks) {
                String packId = pack.getId().toLowerCase();
                String packTitle = pack.getTitle().getString().toLowerCase();

                // Name check
                if (packId.contains("xray") || packId.contains("x-ray") ||
                        packTitle.contains("xray") || packTitle.contains("x-ray")) {
                    setDetected("X-Ray Pack: " + pack.getId(), sendToServer, mc);
                    return;
                }

                // Content inspection for renamed packs
                if (!packId.equals("vanilla") && !packId.contains("mod_")) {
                    try (PackResources resources = pack.open()) {
                        for (ResourceLocation modelLoc : SUSPICIOUS_MODELS) {
                            if (resources.getResource(PackType.CLIENT_RESOURCES, modelLoc) != null) {
                                setDetected("X-Ray Model Modification: " + pack.getId(), sendToServer, mc);
                                return;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (sendToServer && mc.getConnection() != null && !hasReported) {
            PacketDistributor.sendToServer(new XrayDetectedPayload("NONE", getModFileHash()));
            hasReported = true;
        }

        isCheatDetected = false;
        detectedReason = "";
    }

    private static void setDetected(String reason, boolean sendToServer, Minecraft mc) {
        isCheatDetected = true;
        detectedReason = reason;

        if (sendToServer && mc.getConnection() != null) {
            PacketDistributor.sendToServer(new XrayDetectedPayload(reason, getModFileHash()));
            hasReported = true;
        }
    }

    public static boolean isMeteorPresent() {
        try {
            Class.forName("meteordevelopment.meteorclient.MeteorClient");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("baritone.api.BaritoneAPI");
                return true;
            } catch (ClassNotFoundException ignored) {
                return false;
            }
        }
    }

    public static String getModFileHash() {
        if (!cachedModHash.isEmpty()) return cachedModHash;

        try {
            Path modFilePath = ModList.get().getModFileById(ServerCore.MODID).getFile().getFilePath();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream is = Files.newInputStream(modFilePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest.digest()) {
                hexString.append(String.format("%02x", b));
            }

            cachedModHash = hexString.toString();
            return cachedModHash;

        } catch (Exception e) {
            return "ERROR";
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (isCheatDetected) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            Minecraft mc = Minecraft.getInstance();

            String headerText = "DETECTED";
            String subText = detectedReason;

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(3.0F, 3.0F, 3.0F);

            int headerWidth = mc.font.width(headerText);
            float headerX = (screenWidth / 3.0F - headerWidth) / 2.0F;
            float headerY = (screenHeight / 3.0F) / 4.0F;

            guiGraphics.drawString(mc.font, headerText, (int) headerX, (int) headerY, 0xFFFF0000, true);
            guiGraphics.pose().popPose();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(1.5F, 1.5F, 1.5F);

            int subWidth = mc.font.width(subText);
            float subX = (screenWidth / 1.5F - subWidth) / 2.0F;
            float subY = (headerY * 2.0F) + 25.0F;

            guiGraphics.drawString(mc.font, subText, (int) subX, (int) subY, 0xFFFF5555, true);
            guiGraphics.pose().popPose();
        }
    }
}