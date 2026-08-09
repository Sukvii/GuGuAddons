package com.gugucraft.guguaddons.compat.astages;

import com.alessandro.astages.infrastructure.capability.AProvider;
import com.alessandro.astages.infrastructure.capability.BlockOwner;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

/** Accesses the AStages {@code BLOCK_STAGE} attachment used as the machine-owner source of truth. */
public final class AStagesBlockOwner {
    private AStagesBlockOwner() {
    }

    public static UUID getBlockOwner(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getData(AProvider.BLOCK_STAGE).getOwner();
    }

    public static void setBlockOwner(BlockEntity blockEntity, UUID ownerId) {
        if (blockEntity == null || ownerId == null) {
            return;
        }
        BlockOwner stage = blockEntity.getData(AProvider.BLOCK_STAGE);
        stage.setOwner(ownerId);
        blockEntity.setChanged();
    }
}
