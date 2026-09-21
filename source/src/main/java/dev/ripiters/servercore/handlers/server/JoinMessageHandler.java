package dev.ripiters.servercore.handlers.server;

import dev.ripiters.servercore.config.ServerCoreConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinMessageHandler {

    private static final Pattern COLOR_PATTERN = Pattern.compile("(<#[A-Fa-f0-9]{6}>|&[0-9a-fk-or])");

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ServerCoreConfig.COMMON.enableJoinMessage.get()) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            String rawMsg = ServerCoreConfig.COMMON.joinMessage.get();
            rawMsg = rawMsg.replace("%player%", player.getScoreboardName());

            Component formattedMessage = parseToComponent(rawMsg);
            player.server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
        }
    }

    private static Component parseToComponent(String text) {
        MutableComponent root = Component.empty();
        Matcher matcher = COLOR_PATTERN.matcher(text);

        int lastIndex = 0;
        Style currentStyle = Style.EMPTY;

        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                String part = text.substring(lastIndex, matcher.start());
                root.append(Component.literal(part).setStyle(currentStyle));
            }

            String match = matcher.group();
            if (match.startsWith("<#")) {
                String hex = match.substring(1, 8);
                TextColor color = TextColor.parseColor(hex).result().orElse(null);
                if (color != null) {
                    currentStyle = currentStyle.withColor(color);
                }
            } else if (match.startsWith("&")) {
                char code = match.charAt(1);
                currentStyle = applyLegacyCode(currentStyle, code);
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < text.length()) {
            String remaining = text.substring(lastIndex);
            root.append(Component.literal(remaining).setStyle(currentStyle));
        }

        return root;
    }

    private static Style applyLegacyCode(Style style, char code) {
        return switch (code) {
            case '0' -> style.withColor(0x000000);
            case '1' -> style.withColor(0x0000AA);
            case '2' -> style.withColor(0x00AA00);
            case '3' -> style.withColor(0x00AAAA);
            case '4' -> style.withColor(0xAA0000);
            case '5' -> style.withColor(0xAA00AA);
            case '6' -> style.withColor(0xFFAA00);
            case '7' -> style.withColor(0xAAAAAA);
            case '8' -> style.withColor(0x555555);
            case '9' -> style.withColor(0x5555FF);
            case 'a' -> style.withColor(0x55FF55);
            case 'b' -> style.withColor(0x55FFFF);
            case 'c' -> style.withColor(0xFF5555);
            case 'd' -> style.withColor(0xFF55FF);
            case 'e' -> style.withColor(0xFFFF55);
            case 'f' -> style.withColor(0xFFFFFF);
            case 'r' -> Style.EMPTY;
            default -> style;
        };
    }
}