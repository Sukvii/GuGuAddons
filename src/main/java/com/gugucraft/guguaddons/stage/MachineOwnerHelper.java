package com.gugucraft.guguaddons.stage;

import com.gugucraft.guguaddons.compat.astages.AStagesBlockOwner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public final class MachineOwnerHelper {
    public static final String OWNER_KEY = "GuGuAddonsOwner";

    private MachineOwnerHelper() {
    }

    public static UUID getOwner(BlockEntity blockEntity) {
        return AStagesBlockOwner.getBlockOwner(blockEntity);
    }

    public static UUID getOwner(CompoundTag tag) {
        return tag != null && tag.hasUUID(OWNER_KEY) ? tag.getUUID(OWNER_KEY) : null;
    }

    public static void setOwner(BlockEntity blockEntity, UUID ownerId) {
        if (blockEntity == null || ownerId == null) {
            return;
        }
        AStagesBlockOwner.setBlockOwner(blockEntity, ownerId);
        if (blockEntity instanceof MachineOwnerAssignedCallback callback) {
            callback.guguaddons$onMachineOwnerAssigned();
        }
    }

    public static void migrateLegacyOwner(BlockEntity blockEntity, CompoundTag tag) {
        UUID legacyOwner = getOwner(tag);
        if (legacyOwner != null && getOwner(blockEntity) == null) {
            setOwner(blockEntity, legacyOwner);
        }
    }

    public static void setOwner(CompoundTag tag, UUID ownerId) {
        if (tag != null && ownerId != null) {
            tag.putUUID(OWNER_KEY, ownerId);
        }
    }
}
