package dev.ripiters.servercore.network;

import dev.ripiters.servercore.ServerCore;
import dev.ripiters.servercore.network.packet.XrayDetectedPayload;
import dev.ripiters.servercore.util.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// fixme: kick on rp reload + xray, hash, message
public class ServerPayloadHandler {

    public static void handleXrayDetection(final XrayDetectedPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            String playerName = player.getName().getString();
            String reason = payload.reason();

            // Validate mod file hash
            if (payload.fileHash() == null || payload.fileHash().equals("INVALID") || payload.fileHash().equals("ERROR")) {
                Component invalidHashMsg = LanguageManager.get(player, "servercore.kick.invalid_mod_hash");
                if (invalidHashMsg.getString().equals("servercore.kick.invalid_mod_hash")) {
                    invalidHashMsg = Component.literal("§c[Anticheat]\n§fConnection refused: Your ServerCore mod file is modified or outdated.");
                }
                player.connection.disconnect(invalidHashMsg);
                return;
            }

            if (reason == null || reason.equals("NONE")) {
                return;
            }

            // 1. Kick message for player
            Component kickMessage = LanguageManager.get(player, "servercore.kick.cheat_detected", reason);
            if (kickMessage.getString().equals("servercore.kick.cheat_detected")) {
                kickMessage = Component.literal(String.format(
                        "§c[Anticheat]\n§fYou were kicked for using illegal modifications!\n§7Reason: §e%s\n§f§2Hahaha, are you trying to cheat on my server? §f§cI know where you live...",
                        reason
                ));
            }

            // 2. Broadcast message for online players
            Component broadcastMessage = LanguageManager.get(player, "servercore.broadcast.xray_detected", playerName, reason);
            if (broadcastMessage.getString().equals("servercore.broadcast.xray_detected")) {
                broadcastMessage = Component.literal(String.format(
                        "§8[§cAnticheat§8] §cPlayer §e%s §ctried to use X-Ray, but failed miserably and got kicked LOL! §7(%s)",
                        playerName, reason
                ));
            }

            // 3. Server log entry
            Component logComponent = LanguageManager.get(player, "servercore.kick.log.cheat_detected", playerName, reason);
            String logText;
            if (logComponent.getString().equals("servercore.kick.log.cheat_detected")) {
                logText = String.format("§c[Anticheat] §f§cPlayer §e%s has been kicked for detected modifications!\n§7Reason: §e%s", playerName, reason);
            } else {
                logText = logComponent.getString();
            }
            ServerCore.LOGGER.info(logText.replaceAll("§[0-9a-fk-or]", ""));

            // 4. Send chat notification to all players
            if (player.getServer() != null) {
                player.getServer().getPlayerList().broadcastSystemMessage(broadcastMessage, false);
            }

            // 5. Disconnect player
            player.connection.disconnect(kickMessage);
        });
    }
}