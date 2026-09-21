package dev.ripiters.servercore.client;

import dev.ripiters.servercore.ServerCore;
import dev.ripiters.servercore.client.screen.ChangelogScreen;
import dev.ripiters.servercore.util.VersionChecker;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = ServerCore.MODID, value = Dist.CLIENT)
public class ClientUpdateNotifier {
    private static final ResourceLocation REPORT_BUTTON = ResourceLocation.withDefaultNamespace("social_interactions/report_button");
    private static final ResourceLocation REPORT_BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("social_interactions/report_button_highlighted");

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen titleScreen) {
            int buttonSize = 20;
            int x = titleScreen.width / 2 + 104;
            int y = titleScreen.height / 4 + 36;

            Button updateButton = createUpdateButton(x, y, buttonSize);
            updateButton.visible = VersionChecker.isOutdated();
            event.addListener(updateButton);

            int changelogY = y + 74;
            Button changelogButton = Button.builder(
                            Component.literal("CL"),
                            button -> Minecraft.getInstance().setScreen(new ChangelogScreen(titleScreen))
                    )
                    .bounds(x, changelogY, buttonSize, buttonSize)
                    .tooltip(Tooltip.create(Component.literal("Changelog Modpacka")))
                    .build();

            event.addListener(changelogButton);
        }
    }

    private static @NotNull Button createUpdateButton(int x, int y, int buttonSize) {
        Button updateButton = new Button(
                x, y, buttonSize, buttonSize,
                Component.empty(),
                button -> Util.getPlatform().openUri(VersionChecker.getUpdateUrl()),
                messageSupplier -> messageSupplier.get()
        ) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation sprite = this.isHovered() ? REPORT_BUTTON_HIGHLIGHTED : REPORT_BUTTON;
                guiGraphics.blitSprite(sprite, this.getX(), this.getY(), this.width, this.height);
            }
        };

        updateButton.setTooltip(Tooltip.create(
                Component.translatableWithFallback("servercore.update.tooltip", "Modpack Update Available")
        ));
        return updateButton;
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof TitleScreen screen) {
            screen.children().stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .filter(b -> b.getMessage().equals(Component.empty()))
                    .findFirst()
                    .ifPresent(button -> {
                        if (VersionChecker.isOutdated() && !button.visible) {
                            button.visible = true;
                        }
                    });
        }
    }
}