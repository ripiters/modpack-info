package dev.ripiters.servercore.handlers.minecraft;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

@EventBusSubscriber
public class CreateTrainHandler {

    public static final Map<UUID, Long> TRAIN_CREATION_TIMES = new HashMap<>();

    // unused
    public static List<Train> getAllTrains() {
        if (!ModList.get().isLoaded("create")) return Collections.emptyList();
        return CreateHelper.getAllTrains();
    }

    public static int getTrainCountForPlayer(UUID ownerUuid) {
        if (ownerUuid == null || !ModList.get().isLoaded("create")) return 0;
        return CreateHelper.getTrainCountForPlayer(ownerUuid);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ModList.get().isLoaded("create")) return;
        CreateHelper.trackTrains();
    }

    private static class CreateHelper {
        private static List<Train> getAllTrains() {
            return new ArrayList<>(Create.RAILWAYS.trains.values());
        }

        private static int getTrainCountForPlayer(UUID ownerUuid) {
            int count = 0;
            for (Train train : Create.RAILWAYS.trains.values()) {
                if (ownerUuid.equals(train.owner)) {
                    count++;
                }
            }
            return count;
        }

        private static void trackTrains() {
            for (Train train : getAllTrains()) {
                if (!TRAIN_CREATION_TIMES.containsKey(train.id)) {
                    TRAIN_CREATION_TIMES.put(train.id, System.currentTimeMillis());
                }
            }
        }
    }
}