package com.gugucraft.guguaddons.stage;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public final class MachineOwnerHelper {
    private MachineOwnerHelper() {
    }

    public static UUID getOwner(BlockEntity blockEntity) {
        if (blockEntity instanceof MachineOwnerAccess access) {
            return access.guguaddons$getMachineOwner();
        }
        return null;
    }

    public static void setOwner(BlockEntity blockEntity, UUID ownerId) {
        if (blockEntity instanceof MachineOwnerAccess access) {
            access.guguaddons$setMachineOwner(ownerId);
            blockEntity.setChanged();
        }
    }
}
