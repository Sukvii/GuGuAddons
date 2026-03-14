package com.gugucraft.guguaddons.compat.ftbchunks;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChunkClaimDecisionC2SPayload(int sessionId, boolean confirmed) implements CustomPacketPayload {
    public static final Type<ChunkClaimDecisionC2SPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "chunk_claim_decision"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkClaimDecisionC2SPayload> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.VAR_INT, ChunkClaimDecisionC2SPayload::sessionId,
                    ByteBufCodecs.BOOL, ChunkClaimDecisionC2SPayload::confirmed,
                    ChunkClaimDecisionC2SPayload::new);

    @Override
    public Type<ChunkClaimDecisionC2SPayload> type() {
        return TYPE;
    }
}
