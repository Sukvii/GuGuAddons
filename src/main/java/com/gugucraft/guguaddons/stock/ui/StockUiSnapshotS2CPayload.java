package com.gugucraft.guguaddons.stock.ui;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StockUiSnapshotS2CPayload(CompoundTag snapshotTag) implements CustomPacketPayload {
    public static final Type<StockUiSnapshotS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "stock_ui_snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StockUiSnapshotS2CPayload> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.COMPOUND_TAG, StockUiSnapshotS2CPayload::snapshotTag,
                    StockUiSnapshotS2CPayload::new);

    public StockUiSnapshotS2CPayload(StockUiSnapshot snapshot) {
        this(snapshot.toTag());
    }

    public StockUiSnapshot snapshot() {
        return StockUiSnapshot.fromTag(snapshotTag == null ? new CompoundTag() : snapshotTag.copy());
    }

    @Override
    public Type<StockUiSnapshotS2CPayload> type() {
        return TYPE;
    }
}
