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

    private final Map<ChunkKey, ClaimPayment> payments = new HashMap<>();

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

    public record ClaimPayment(UUID payerId, int paidAmount) {
    }

    private record ChunkKey(ResourceKey<Level> dimension, int x, int z) {
        private static ChunkKey from(ChunkDimPos pos) {
            return new ChunkKey(pos.dimension(), pos.x(), pos.z());
        }
    }
}
