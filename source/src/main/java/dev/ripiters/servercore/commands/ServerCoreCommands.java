package dev.ripiters.servercore.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ripiters.servercore.config.ServerCoreConfig;
import dev.ripiters.servercore.handlers.server.ProtectionManager;
import dev.ripiters.servercore.handlers.server.SpectateHandler;
import dev.ripiters.servercore.util.LanguageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber
public class ServerCoreCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /protect <password> <confirm_password>
        dispatcher.register(Commands.literal("protect")
                .then(Commands.argument("password", StringArgumentType.string())
                        .then(Commands.argument("confirm_password", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String pass1 = StringArgumentType.getString(ctx, "password");
                                    String pass2 = StringArgumentType.getString(ctx, "confirm_password");
                                    ProtectionManager.setPassword(player, pass1, pass2);
                                    return 1;
                                }))));

        // /unprotect <password> <confirm_password>
        dispatcher.register(Commands.literal("unprotect")
                .then(Commands.argument("password", StringArgumentType.string())
                        .then(Commands.argument("confirm_password", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String pass1 = StringArgumentType.getString(ctx, "password");
                                    String pass2 = StringArgumentType.getString(ctx, "confirm_password");
                                    ProtectionManager.removePassword(player, pass1, pass2);
                                    return 1;
                                }))));

        // /login <password>
        dispatcher.register(Commands.literal("login")
                .then(Commands.argument("password", StringArgumentType.string())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String pass = StringArgumentType.getString(ctx, "password");
                            ProtectionManager.login(player, pass);
                            return 1;
                        })));

        // /admin commands
        dispatcher.register(Commands.literal("admin")
                .requires(source -> checkAdminPermission(source))
                .then(Commands.literal("spectate")
                        .then(Commands.literal("full")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer admin = ctx.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            SpectateHandler.startSpectating(admin, target, true);
                                            return 1;
                                        })))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer admin = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    SpectateHandler.startSpectating(admin, target, false);
                                    return 1;
                                })))
                .then(Commands.literal("unspectate")
                        .executes(ctx -> {
                            ServerPlayer admin = ctx.getSource().getPlayerOrException();
                            SpectateHandler.stopSpectating(admin);
                            return 1;
                        }))
                .then(Commands.literal("enderchest")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer admin = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    SpectateHandler.openTargetEnderChest(admin, target);
                                    return 1;
                                })))
                .then(Commands.literal("inventory")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer admin = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    SpectateHandler.openTargetInventory(admin, target);
                                    return 1;
                                })))
                .then(Commands.literal("inv")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer admin = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    SpectateHandler.openTargetInventory(admin, target);
                                    return 1;
                                })))
                .then(Commands.literal("stats")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer admin = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    ctx.getSource().sendSuccess(() -> LanguageManager.get(
                                            admin,
                                            "servercore.command.stats",
                                            target.getScoreboardName(),
                                            target.getHealth(),
                                            target.getMaxHealth(),
                                            target.getFoodData().getFoodLevel(),
                                            target.connection.latency(),
                                            target.getX(),
                                            target.getY(),
                                            target.getZ(),
                                            target.level().dimension().location().toString()
                                    ), false);
                                    return 1;
                                }))));

        dispatcher.register(Commands.literal("rconips")
                .requires(source -> !source.isPlayer() || isAllowedAdmin(source))
                .executes(ctx -> {
                    StringBuilder sb = new StringBuilder("PLAYERS_IP_LIST:\n");
                    for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                        String cleanIp = ProtectionManager.getCleanIp(player);
                        sb.append(String.format("%s=%s\n", player.getScoreboardName(), cleanIp));
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                    return 1;
                }));

        dispatcher.register(Commands.literal("rconchunks")
                .requires(source -> !source.isPlayer() || isAllowedAdmin(source))
                .executes(ctx -> {
                    double mspt = ctx.getSource().getServer().getAverageTickTimeNanos() / 1_000_000.0;
                    double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 1.0));

                    StringBuilder json = new StringBuilder("CHUNK_STATS_JSON:{");
                    json.append(String.format("\"mspt\":%.2f,\"tps\":%.2f,\"dimensions\":[", mspt, tps));

                    boolean firstDim = true;
                    for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
                        if (!firstDim) json.append(",");
                        firstDim = false;

                        String dimId = level.dimension().location().toString();
                        int loadedChunks = level.getChunkSource().getLoadedChunksCount();
                        int forcedChunks = level.getForcedChunks().size();

                        Map<ChunkPos, Integer> entityCounts = new HashMap<>();
                        int totalEntities = 0;
                        for (Entity entity : level.getAllEntities()) {
                            ChunkPos pos = entity.chunkPosition();
                            entityCounts.put(pos, entityCounts.getOrDefault(pos, 0) + 1);
                            totalEntities++;
                        }

                        json.append("{")
                                .append("\"id\":\"").append(dimId).append("\",")
                                .append("\"loadedChunks\":").append(loadedChunks).append(",")
                                .append("\"forcedChunks\":").append(forcedChunks).append(",")
                                .append("\"totalEntities\":").append(totalEntities).append(",")
                                .append("\"hotspots\":[");

                        List<Map.Entry<ChunkPos, Integer>> topHotspots = entityCounts.entrySet().stream()
                                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                                .limit(5)
                                .toList();

                        boolean firstHotspot = true;
                        for (Map.Entry<ChunkPos, Integer> entry : topHotspots) {
                            if (!firstHotspot) json.append(",");
                            firstHotspot = false;

                            ChunkPos pos = entry.getKey();
                            int count = entry.getValue();
                            int blockX = pos.getMiddleBlockX();
                            int blockZ = pos.getMiddleBlockZ();

                            json.append(String.format("{\"chunkX\":%d,\"chunkZ\":%d,\"blockX\":%d,\"blockZ\":%d,\"entities\":%d}",
                                    pos.x, pos.z, blockX, blockZ, count));
                        }
                        json.append("]}");
                    }
                    json.append("]}");

                    ctx.getSource().sendSuccess(() -> Component.literal(json.toString()), false);
                    return 1;
                }));
    }

    private static boolean checkAdminPermission(CommandSourceStack source) {
        if (!source.isPlayer()) return true;
        return isAllowedAdmin(source) || source.hasPermission(2);
    }

    @SubscribeEvent
    public static void onCommandExecute(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        String command = event.getParseResults().getReader().getString().trim();

        if (isProtectedCreateTrainCommand(command)) {
            if (!isAllowedAdmin(source)) {
                event.setCanceled(true);
                ServerPlayer player = source.getPlayer();
                if (player != null) {
                    source.sendFailure(LanguageManager.get(player, "servercore.command.train_no_permission"));
                }
            }
        }
    }

    private static boolean isProtectedCreateTrainCommand(String command) {
        String lower = command.toLowerCase();
        if (lower.startsWith("/")) {
            lower = lower.substring(1);
        }
        return lower.equals("create train") || lower.startsWith("create train ")
                || lower.equals("create trains") || lower.startsWith("create trains ");
    }

    public static boolean isAllowedAdmin(CommandSourceStack source) {
        if (!source.isPlayer()) return true;
        ServerPlayer player = source.getPlayer();
        if (player == null) return false;

        List<? extends String> allowed = ServerCoreConfig.COMMON.allowedAdmins.get();
        if (allowed == null || allowed.isEmpty()) return false;

        String uuid = player.getUUID().toString();
        String name = player.getScoreboardName();

        for (String entry : allowed) {
            if (entry.equalsIgnoreCase(uuid) || entry.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}