package dev.ripiters.servercore.mixin.create;

import com.simibubi.create.content.trains.station.StationBlockEntity;
import dev.ripiters.servercore.config.ServerCoreConfig;
import dev.ripiters.servercore.handlers.minecraft.CreateTrainHandler;
import dev.ripiters.servercore.util.LanguageManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = StationBlockEntity.class, remap = false)
public class StationBlockEntityMixin {

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
    private void preventExceedingTrainLimit(UUID playerUUID, CallbackInfo ci) {
        if (playerUUID == null) return;

        StationBlockEntity station = (StationBlockEntity) (Object) this;
        if (station.getLevel() == null || station.getLevel().getServer() == null) return;

        ServerPlayer player = station.getLevel().getServer().getPlayerList().getPlayer(playerUUID);

        if (player != null && player.hasPermissions(2)) {
            return;
        }

        int max = ServerCoreConfig.COMMON.maxTrainsPerPlayer.get();
        int currentCount = CreateTrainHandler.getTrainCountForPlayer(playerUUID);

        if (currentCount >= max) {
            ci.cancel();

            if (player != null) {
                player.sendSystemMessage(LanguageManager.get(player, "servercore.train.limit_reached", max));
            }
        }
    }
}