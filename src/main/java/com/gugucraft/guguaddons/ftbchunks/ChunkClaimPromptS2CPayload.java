package com.gugucraft.guguaddons.ftbchunks;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChunkClaimPromptS2CPayload(int sessionId, int chunkCount, int totalCost) implements CustomPacketPayload {
    public static final Type<ChunkClaimPromptS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "chunk_claim_prompt"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkClaimPromptS2CPayload> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.VAR_INT, ChunkClaimPromptS2CPayload::sessionId,
                    ByteBufCodecs.VAR_INT, ChunkClaimPromptS2CPayload::chunkCount,
                    ByteBufCodecs.VAR_INT, ChunkClaimPromptS2CPayload::totalCost,
                    ChunkClaimPromptS2CPayload::new);

    @Override
    public Type<ChunkClaimPromptS2CPayload> type() {
        return TYPE;
    }
}
