package dev.ripiters.servercore.handlers.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ripiters.servercore.util.LanguageManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

@EventBusSubscriber
public class ProtectionManager {

    private static final File STORAGE_FILE = new File("config/servercore_passwords.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class AccountData {
        public String password;
        public String lastIp;

        public AccountData(String password, String lastIp) {
            this.password = password;
            this.lastIp = lastIp;
        }
    }

    private static Map<UUID, AccountData> ACCOUNTS = new HashMap<>();
    private static final Set<UUID> PENDING_AUTH = new HashSet<>();

    static {
        loadData();
    }

    public static synchronized void loadData() {
        if (!STORAGE_FILE.exists()) {
            ACCOUNTS = new HashMap<>();
            return;
        }
        try (FileReader reader = new FileReader(STORAGE_FILE)) {
            Type type = new TypeToken<Map<UUID, AccountData>>() {}.getType();
            Map<UUID, AccountData> data = GSON.fromJson(reader, type);
            if (data != null) {
                ACCOUNTS = data;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void saveData() {
        try {
            if (!STORAGE_FILE.getParentFile().exists()) {
                STORAGE_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(STORAGE_FILE)) {
                GSON.toJson(ACCOUNTS, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCleanIp(ServerPlayer player) {
        String rawIp = player.connection.getRemoteAddress().toString();
        if (rawIp.startsWith("/")) {
            rawIp = rawIp.substring(1);
        }
        int colonIdx = rawIp.indexOf(':');
        if (colonIdx != -1) {
            rawIp = rawIp.substring(0, colonIdx);
        }
        return rawIp;
    }

    public static boolean isProtected(UUID uuid) {
        return ACCOUNTS.containsKey(uuid);
    }

    public static boolean isPendingAuth(UUID uuid) {
        return PENDING_AUTH.contains(uuid);
    }

    public static boolean setPassword(ServerPlayer player, String pass1, String pass2) {
        if (!pass1.equals(pass2)) {
            player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.passwords_not_match"));
            return false;
        }
        if (pass1.length() > 15) {
            player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.password_too_long"));
            return false;
        }

        String ip = getCleanIp(player);
        ACCOUNTS.put(player.getUUID(), new AccountData(pass1, ip));
        PENDING_AUTH.remove(player.getUUID());
        saveData();
        player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.enabled"));
        return true;
    }

    public static boolean removePassword(ServerPlayer player, String pass1, String pass2) {
        if (!pass1.equals(pass2)) {
            player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.passwords_not_match"));
            return false;
        }

        UUID uuid = player.getUUID();
        AccountData data = ACCOUNTS.get(uuid);
        if (data == null) {
            player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.not_protected"));
            return false;
        }

        if (!data.password.equals(pass1)) {
            player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.wrong_password"));
            return false;
        }

        ACCOUNTS.remove(uuid);
        PENDING_AUTH.remove(uuid);
        saveData();
        player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.disabled"));
        return true;
    }

    public static boolean login(ServerPlayer player, String password) {
        UUID uuid = player.getUUID();
        AccountData data = ACCOUNTS.get(uuid);
        if (data == null) {
            player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.not_protected"));
            return false;
        }

        if (!data.password.equals(password)) {
            player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.wrong_password"));
            return false;
        }

        data.lastIp = getCleanIp(player);
        PENDING_AUTH.remove(uuid);
        saveData();
        player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.login_success"));
        return true;
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            if (isProtected(uuid)) {
                AccountData data = ACCOUNTS.get(uuid);
                String currentIp = getCleanIp(player);
                if (!currentIp.equals(data.lastIp)) {
                    PENDING_AUTH.add(uuid);
                    player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.ip_changed"));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_AUTH.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onCommandExecute(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
            if (isPendingAuth(player.getUUID())) {
                String cmd = event.getParseResults().getReader().getString().trim().toLowerCase();
                if (!cmd.startsWith("/login") && !cmd.startsWith("login")) {
                    event.setCanceled(true);
                    player.sendSystemMessage(LanguageManager.get(player, "servercore.protection.must_login"));
                }
            }
        }
    }

    // Cancel interaction events for unauthenticated players
    private static void cancelIfPending(PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isPendingAuth(player.getUUID())) {
            if (event instanceof ICancellableEvent cancellable) {
                cancellable.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfPending(event);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelIfPending(event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelIfPending(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfPending(event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelIfPending(event);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        saveData();
    }
}