package dev.ripiters.servercore.mixin.mmv;

import com.github.nyuppo.networking.ClientRequestVariantPayload;
import com.github.nyuppo.networking.ServerRespondBasicVariantPayload;
import com.github.nyuppo.networking.ServerRespondVariantPayload;
import dev.ripiters.servercore.util.MMVClientPacketProxy;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.nyuppo.MoreMobVariants", remap = false)
public abstract class MoreMobVariantsFixMixin {

    @Shadow
    private static void handleClientRequest(ClientRequestVariantPayload payload, IPayloadContext context) {}

    @Inject(method = "registerPayloads", at = @At("HEAD"), cancellable = true)
    private void fixClientPayloadRegistration(RegisterPayloadHandlersEvent event, CallbackInfo ci) {
        ci.cancel();
        PayloadRegistrar registrar = event.registrar("moremobvariants");
        registrar.playToServer(ClientRequestVariantPayload.TYPE, ClientRequestVariantPayload.STREAM_CODEC, MoreMobVariantsFixMixin::handleClientRequest);

        registrar.playToClient(ServerRespondBasicVariantPayload.TYPE, ServerRespondBasicVariantPayload.STREAM_CODEC, (payload, context) -> {
                    if (FMLEnvironment.dist.isClient()) {
                        MMVClientPacketProxy.handleServerRespondBasic(payload, context);
                    }
                }
        );

        registrar.playToClient(ServerRespondVariantPayload.TYPE, ServerRespondVariantPayload.STREAM_CODEC, (payload, context) -> {
                    if (FMLEnvironment.dist.isClient()) {
                        MMVClientPacketProxy.handleServerRespond(payload, context);
                    }
                }
        );
    }
}