package com.gugucraft.guguaddons.stock.ui;

import java.util.UUID;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StockUiActionC2SPayload(
        UUID sessionNonce,
        long requestId,
        int actionId,
        int targetStock) implements CustomPacketPayload {
    public static final Type<StockUiActionC2SPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "stock_ui_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StockUiActionC2SPayload> STREAM_CODEC = StreamCodec
            .composite(
                    UUIDUtil.STREAM_CODEC, StockUiActionC2SPayload::sessionNonce,
                    ByteBufCodecs.VAR_LONG, StockUiActionC2SPayload::requestId,
                    ByteBufCodecs.VAR_INT, StockUiActionC2SPayload::actionId,
                    ByteBufCodecs.VAR_INT, StockUiActionC2SPayload::targetStock,
                    StockUiActionC2SPayload::new);

    public StockUiAction action() {
        return StockUiAction.fromId(actionId);
    }

    @Override
    public Type<StockUiActionC2SPayload> type() {
        return TYPE;
    }
}
