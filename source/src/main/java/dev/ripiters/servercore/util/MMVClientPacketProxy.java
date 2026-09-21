package dev.ripiters.servercore.util;

import com.github.nyuppo.MoreMobVariantsClient;
import com.github.nyuppo.networking.ServerRespondBasicVariantPayload;
import com.github.nyuppo.networking.ServerRespondVariantPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MMVClientPacketProxy {

    // Safely invokes client handling code only when executed on physical client
    public static void handleServerRespondBasic(ServerRespondBasicVariantPayload payload, IPayloadContext context) {
        MoreMobVariantsClient.handleServerRespondBasic(payload, context);
    }

    // Safely invokes client handling code only when executed on physical client
    public static void handleServerRespond(ServerRespondVariantPayload payload, IPayloadContext context) {
        MoreMobVariantsClient.handleServerRespond(payload, context);
    }
}