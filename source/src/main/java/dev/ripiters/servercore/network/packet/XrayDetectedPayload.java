package dev.ripiters.servercore.network.packet;

import dev.ripiters.servercore.ServerCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record XrayDetectedPayload(String reason, String fileHash) implements CustomPacketPayload {

    public static final Type<XrayDetectedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ServerCore.MODID, "xray_detected"));

    public static final StreamCodec<FriendlyByteBuf, XrayDetectedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            XrayDetectedPayload::reason,
            ByteBufCodecs.STRING_UTF8,
            XrayDetectedPayload::fileHash,
            XrayDetectedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}