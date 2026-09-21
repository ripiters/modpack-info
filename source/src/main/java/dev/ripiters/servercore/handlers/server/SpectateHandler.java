package dev.ripiters.servercore.handlers.server;

import dev.ripiters.servercore.util.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpectateHandler {

    private record SavedState(
            double x, double y, double z,
            float yRot, float xRot,
            ServerLevel level,
            GameType gameMode,
            UUID targetUuid,
            ArmorStand dummyEntity
    ) {}

    private static final Map<UUID, SavedState> SPECTATING_ADMINS = new HashMap<>();

    public static void startSpectating(ServerPlayer admin, ServerPlayer target, boolean fullBody) {
        if (admin.equals(target)) return;

        if (isSpectating(admin.getUUID())) {
            stopSpectating(admin);
        }

        ArmorStand dummy = null;
        if (fullBody) {
            dummy = new ArmorStand(EntityType.ARMOR_STAND, admin.serverLevel());
            dummy.setPos(admin.getX(), admin.getY(), admin.getZ());
            dummy.setYRot(admin.getYRot());
            dummy.setXRot(admin.getXRot());
            dummy.setCustomName(admin.getDisplayName());
            dummy.setCustomNameVisible(true);
            dummy.setInvulnerable(true);
            dummy.setNoGravity(true);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                dummy.setItemSlot(slot, admin.getItemBySlot(slot).copy());
            }
            admin.serverLevel().addFreshEntity(dummy);
        }

        SPECTATING_ADMINS.put(admin.getUUID(), new SavedState(
                admin.getX(), admin.getY(), admin.getZ(),
                admin.getYRot(), admin.getXRot(),
                admin.serverLevel(),
                admin.gameMode.getGameModeForPlayer(),
                target.getUUID(),
                dummy
        ));

        admin.setGameMode(GameType.SPECTATOR);
        admin.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        admin.setCamera(target);

        admin.sendSystemMessage(LanguageManager.get(admin, "servercore.spectate.start", target.getScoreboardName()));
    }

    public static void stopSpectating(ServerPlayer admin) {
        SavedState state = SPECTATING_ADMINS.remove(admin.getUUID());
        admin.setCamera(admin);

        if (state != null) {
            if (state.dummyEntity() != null && state.dummyEntity().isAlive()) {
                state.dummyEntity().discard();
            }
            admin.setGameMode(state.gameMode());
            admin.teleportTo(state.level(), state.x(), state.y(), state.z(), state.yRot(), state.xRot());
        }

        admin.sendSystemMessage(LanguageManager.get(admin, "servercore.spectate.stop"));
    }

    public static boolean isSpectating(UUID adminUuid) {
        return SPECTATING_ADMINS.containsKey(adminUuid);
    }

    private static void handleTargetUnavailable(ServerPlayer target) {
        UUID targetUuid = target.getUUID();
        List<UUID> adminsToStop = new ArrayList<>();

        for (Map.Entry<UUID, SavedState> entry : SPECTATING_ADMINS.entrySet()) {
            if (entry.getValue().targetUuid().equals(targetUuid)) {
                adminsToStop.add(entry.getKey());
            }
        }

        for (UUID adminUuid : adminsToStop) {
            ServerPlayer admin = target.server.getPlayerList().getPlayer(adminUuid);
            if (admin != null) {
                stopSpectating(admin);
            } else {
                SavedState state = SPECTATING_ADMINS.remove(adminUuid);
                if (state != null && state.dummyEntity() != null) {
                    state.dummyEntity().discard();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleTargetUnavailable(player);
            if (SPECTATING_ADMINS.containsKey(player.getUUID())) {
                stopSpectating(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleTargetUnavailable(player);
        }
    }

    public static void openTargetEnderChest(ServerPlayer admin, ServerPlayer target) {
        PlayerEnderChestContainer enderChest = target.getEnderChestInventory();
        admin.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return LanguageManager.get(admin, "servercore.container.ender_chest", target.getScoreboardName());
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return ChestMenu.threeRows(id, inv, enderChest);
            }
        });
    }

    public static void openTargetInventory(ServerPlayer admin, ServerPlayer target) {
        admin.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return LanguageManager.get(admin, "servercore.container.inventory", target.getScoreboardName());
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory adminInv, Player p) {
                Container targetContainer = new Container() {
                    @Override
                    public int getContainerSize() {
                        return 54;
                    }

                    @Override
                    public boolean isEmpty() {
                        return target.getInventory().isEmpty();
                    }

                    @Override
                    public ItemStack getItem(int slot) {
                        if (slot < 36) {
                            return target.getInventory().getItem(slot);
                        } else if (slot >= 36 && slot < 40) {
                            return target.getInventory().armor.get(slot - 36);
                        } else if (slot == 40) {
                            return target.getInventory().offhand.get(0);
                        }
                        return ItemStack.EMPTY;
                    }

                    @Override
                    public ItemStack removeItem(int slot, int amount) {
                        ItemStack stack = getItem(slot);
                        if (stack.isEmpty()) return ItemStack.EMPTY;
                        ItemStack split = stack.split(amount);
                        setChanged();
                        return split;
                    }

                    @Override
                    public ItemStack removeItemNoUpdate(int slot) {
                        ItemStack stack = getItem(slot);
                        if (stack.isEmpty()) return ItemStack.EMPTY;
                        setItem(slot, ItemStack.EMPTY);
                        return stack;
                    }

                    @Override
                    public void setItem(int slot, ItemStack stack) {
                        if (slot < 36) {
                            target.getInventory().setItem(slot, stack);
                        } else if (slot >= 36 && slot < 40) {
                            target.getInventory().armor.set(slot - 36, stack);
                        } else if (slot == 40) {
                            target.getInventory().offhand.set(0, stack);
                        }
                        setChanged();
                    }

                    @Override
                    public void setChanged() {
                        target.getInventory().setChanged();
                    }

                    @Override
                    public boolean stillValid(Player player) {
                        return target.isAlive();
                    }

                    @Override
                    public void clearContent() {
                        target.getInventory().clearContent();
                    }
                };

                return ChestMenu.sixRows(id, adminInv, targetContainer);
            }
        });
    }
}