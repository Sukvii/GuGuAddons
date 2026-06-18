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
        if (blockEntity instanceof MachineOwnerAccess access) {
            return access.guguaddons$getMachineOwner();
        }
        return null;
    }

    public static UUID getOwner(CompoundTag tag) {
        return tag != null && tag.hasUUID(OWNER_KEY) ? tag.getUUID(OWNER_KEY) : null;
    }

    public static void setOwner(BlockEntity blockEntity, UUID ownerId) {
        if (blockEntity instanceof MachineOwnerAccess access) {
            access.guguaddons$setMachineOwner(ownerId);
            blockEntity.setChanged();
            AStagesBlockOwner.setBlockOwner(blockEntity, ownerId);
            if (blockEntity instanceof MachineOwnerAssignedCallback callback) {
                callback.guguaddons$onMachineOwnerAssigned();
            }
        }
    }

    public static void setOwner(CompoundTag tag, UUID ownerId) {
        if (tag != null && ownerId != null) {
            tag.putUUID(OWNER_KEY, ownerId);
        }
    }
}
