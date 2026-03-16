package com.gugucraft.guguaddons;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConfigSyncS2CPayload(ServerConfigSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<ConfigSyncS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "config_snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncS2CPayload> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.STRING_UTF8, payload -> payload.snapshot().slashBackRefillTemplate(),
                    ByteBufCodecs.STRING_UTF8, payload -> payload.snapshot().slashBackRefillMaterial(),
                    ByteBufCodecs.VAR_INT, payload -> payload.snapshot().ftbChunksClaimPrice(),
                    ByteBufCodecs.DOUBLE, payload -> payload.snapshot().ftbChunksUnclaimRefundRatio(),
                    ByteBufCodecs.VAR_INT, payload -> payload.snapshot().ftbChunksClaimConfirmTimeoutTicks(),
                    ByteBufCodecs.BOOL, payload -> payload.snapshot().stockEnabled(),
                    (slashBackRefillTemplate,
                            slashBackRefillMaterial,
                            ftbChunksClaimPrice,
                            ftbChunksUnclaimRefundRatio,
                            ftbChunksClaimConfirmTimeoutTicks,
                            stockEnabled) -> new ConfigSyncS2CPayload(
                                    new ServerConfigSnapshot(
                                            slashBackRefillTemplate,
                                            slashBackRefillMaterial,
                                            ftbChunksClaimPrice,
                                            ftbChunksUnclaimRefundRatio,
                                            ftbChunksClaimConfirmTimeoutTicks,
                                            stockEnabled)));

    @Override
    public Type<ConfigSyncS2CPayload> type() {
        return TYPE;
    }
}
