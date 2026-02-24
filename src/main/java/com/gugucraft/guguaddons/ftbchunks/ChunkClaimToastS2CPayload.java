package com.gugucraft.guguaddons.ftbchunks;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChunkClaimToastS2CPayload(String messageKey, int argCount, String arg1, String arg2, String arg3)
        implements CustomPacketPayload {
    public static final Type<ChunkClaimToastS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "chunk_claim_toast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkClaimToastS2CPayload> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.STRING_UTF8, ChunkClaimToastS2CPayload::messageKey,
                    ByteBufCodecs.VAR_INT, ChunkClaimToastS2CPayload::argCount,
                    ByteBufCodecs.STRING_UTF8, ChunkClaimToastS2CPayload::arg1,
                    ByteBufCodecs.STRING_UTF8, ChunkClaimToastS2CPayload::arg2,
                    ByteBufCodecs.STRING_UTF8, ChunkClaimToastS2CPayload::arg3,
                    ChunkClaimToastS2CPayload::new);

    @Override
    public Type<ChunkClaimToastS2CPayload> type() {
        return TYPE;
    }
}
