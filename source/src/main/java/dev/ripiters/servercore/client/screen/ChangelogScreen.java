package dev.ripiters.servercore.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChangelogScreen extends Screen {

    private final Screen lastScreen;
    private final List<Component> formattedLines = new ArrayList<>();
    private double scrollAmount = 0;
    private int totalHeight = 0;

    public ChangelogScreen(Screen lastScreen) {
        super(Component.translatable("servercore.gui.changelog.title"));
        this.lastScreen = lastScreen;
        loadChangelog();
    }

    private void loadChangelog() {
        String langCode = Minecraft.getInstance().getLanguageManager().getSelected();
        boolean isPolish = langCode.toLowerCase().startsWith("pl");
        String langSuffix = isPolish ? "pl" : "en";

        Path baseDir = FMLPaths.CONFIGDIR.get().resolve("modpack");

        try {
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Path fileToRead = baseDir.resolve("changelog_" + langSuffix + ".md");

        if (!Files.exists(fileToRead)) {
            fileToRead = baseDir.resolve("changelog.md");
        }

        if (Files.exists(fileToRead)) {
            try (BufferedReader reader = Files.newBufferedReader(fileToRead, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    formattedLines.add(parseMarkdownLine(line));
                }
            } catch (Exception e) {
                formattedLines.add(Component.translatable("servercore.gui.changelog.error", e.getMessage()));
            }
        } else {
            if (isPolish) {
                formattedLines.add(Component.literal("§cBrak pliku changelogu!"));
            } else {
                formattedLines.add(Component.literal("§cChangelog file not found!"));
            }
        }
    }

    private Component parseMarkdownLine(String line) {
        if (line.startsWith("# ")) {
            return Component.literal("§l§6" + line.substring(2));
        } else if (line.startsWith("## ")) {
            return Component.literal("§l§b" + line.substring(3));
        } else if (line.startsWith("### ")) {
            return Component.literal("§e" + line.substring(4));
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            return Component.literal(" §7• §f" + line.substring(2));
        } else if (line.trim().isEmpty()) {
            return Component.literal("");
        }
        return Component.literal("§f" + line);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.translatable("servercore.gui.changelog.close"), button -> this.minecraft.setScreen(this.lastScreen))
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, totalHeight - (this.height - 70));
        this.scrollAmount = Math.clamp(this.scrollAmount - scrollY * 12, 0, maxScroll);
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render background panel
        int left = this.width / 2 - 160;
        int top = 30;
        int right = this.width / 2 + 160;
        int bottom = this.height - 35;

        guiGraphics.fill(left - 2, top - 2, right + 2, bottom + 2, 0xFF000000);
        guiGraphics.fill(left, top, right, bottom, 0xC0101010);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        // Enable Scissor test for scrolling content
        guiGraphics.enableScissor(left, top, right, bottom);

        int currentY = top + 8 - (int) this.scrollAmount;
        for (Component comp : formattedLines) {
            if (currentY + 10 >= top && currentY <= bottom) {
                guiGraphics.drawString(this.font, comp, left + 10, currentY, 0xFFFFFF, false);
            }
            currentY += 12;
        }

        totalHeight = currentY + (int) this.scrollAmount - top;
        guiGraphics.disableScissor();
    }
}