package com.gugucraft.guguaddons.compat.ftbchunks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class ChunkClaimEconomySavedData extends SavedData {
    private static final String DATA_NAME = "guguaddons_chunk_claim_economy";
    private static final String CLAIMS_TAG = "Claims";
    private static final String PENDING_REFUNDS_TAG = "PendingRefunds";

    private final Map<ChunkKey, ClaimPayment> payments = new HashMap<>();
    private final Map<UUID, Long> pendingRefunds = new HashMap<>();

    public static SavedData.Factory<ChunkClaimEconomySavedData> factory() {
        return new SavedData.Factory<>(ChunkClaimEconomySavedData::new, ChunkClaimEconomySavedData::load, null);
    }

    public static ChunkClaimEconomySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    private static ChunkClaimEconomySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ChunkClaimEconomySavedData data = new ChunkClaimEconomySavedData();
        ListTag list = tag.getList(CLAIMS_TAG, Tag.TAG_COMPOUND);

        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag claimTag) || !claimTag.hasUUID("Payer")) {
                continue;
            }

            ResourceLocation dimId = ResourceLocation.tryParse(claimTag.getString("Dim"));
            if (dimId == null) {
                continue;
            }

            ChunkKey key = new ChunkKey(
                    ResourceKey.create(Registries.DIMENSION, dimId),
                    claimTag.getInt("X"),
                    claimTag.getInt("Z"));
            ClaimPayment payment = new ClaimPayment(claimTag.getUUID("Payer"), Math.max(0, claimTag.getInt("Paid")));
            data.payments.put(key, payment);
        }

        ListTag pendingRefundList = tag.getList(PENDING_REFUNDS_TAG, Tag.TAG_COMPOUND);
        for (Tag raw : pendingRefundList) {
            if (!(raw instanceof CompoundTag refundTag) || !refundTag.hasUUID("Payer")) {
                continue;
            }

            long amount = refundTag.getLong("Amount");
            if (amount > 0L) {
                data.pendingRefunds.merge(refundTag.getUUID("Payer"), amount,
                        ChunkClaimEconomySavedData::saturatedAdd);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<ChunkKey, ClaimPayment> entry : payments.entrySet()) {
            ChunkKey key = entry.getKey();
            ClaimPayment payment = entry.getValue();

            CompoundTag claimTag = new CompoundTag();
            claimTag.putString("Dim", key.dimension.location().toString());
            claimTag.putInt("X", key.x);
            claimTag.putInt("Z", key.z);
            claimTag.putUUID("Payer", payment.payerId);
            claimTag.putInt("Paid", payment.paidAmount);
            list.add(claimTag);
        }

        tag.put(CLAIMS_TAG, list);

        ListTag pendingRefundList = new ListTag();
        for (Map.Entry<UUID, Long> entry : pendingRefunds.entrySet()) {
            long amount = entry.getValue();
            if (amount <= 0L) {
                continue;
            }

            CompoundTag refundTag = new CompoundTag();
            refundTag.putUUID("Payer", entry.getKey());
            refundTag.putLong("Amount", amount);
            pendingRefundList.add(refundTag);
        }
        tag.put(PENDING_REFUNDS_TAG, pendingRefundList);
        return tag;
    }

    public void recordClaim(ChunkDimPos pos, UUID payerId, int paidAmount) {
        int paid = Math.max(0, paidAmount);
        if (paid == 0) {
            return;
        }

        payments.put(ChunkKey.from(pos), new ClaimPayment(payerId, paid));
        setDirty();
    }

    public Optional<ClaimPayment> removeClaim(ChunkDimPos pos) {
        ClaimPayment removed = payments.remove(ChunkKey.from(pos));
        if (removed != null) {
            setDirty();
        }
        return Optional.ofNullable(removed);
    }

    public void addPendingRefund(UUID payerId, long amount) {
        if (amount <= 0L) {
            return;
        }
        pendingRefunds.merge(payerId, amount, ChunkClaimEconomySavedData::saturatedAdd);
        setDirty();
    }

    public Map<UUID, Long> getPendingRefundsSnapshot() {
        return Map.copyOf(pendingRefunds);
    }

    public void consumePendingRefund(UUID payerId, long amount) {
        if (amount <= 0L) {
            return;
        }

        Long pending = pendingRefunds.get(payerId);
        if (pending == null || pending <= 0L) {
            return;
        }
        if (amount >= pending) {
            pendingRefunds.remove(payerId);
        } else {
            pendingRefunds.put(payerId, pending - amount);
        }
        setDirty();
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public record ClaimPayment(UUID payerId, int paidAmount) {
    }

    private record ChunkKey(ResourceKey<Level> dimension, int x, int z) {
        private static ChunkKey from(ChunkDimPos pos) {
            return new ChunkKey(pos.dimension(), pos.x(), pos.z());
        }
    }
}
