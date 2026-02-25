package com.gugucraft.guguaddons.stock.ui;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StockUiActionC2SPayload(
        int actionId,
        int page,
        int selectedStock,
        int lotIndex,
        int windowIndex,
        int targetStock) implements CustomPacketPayload {
    public static final Type<StockUiActionC2SPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "stock_ui_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StockUiActionC2SPayload> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.VAR_INT, StockUiActionC2SPayload::actionId,
                    ByteBufCodecs.VAR_INT, StockUiActionC2SPayload::page,
                    ByteBufCodecs.VAR_INT, StockUiActionC2SPayload::selectedStock,
                    ByteBufCodecs.VAR_INT, StockUiActionC2SPayload::lotIndex,
                    ByteBufCodecs.VAR_INT, StockUiActionC2SPayload::windowIndex,
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
