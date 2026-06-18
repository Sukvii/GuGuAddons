package com.gugucraft.guguaddons.compat.astages;

import com.alessandro.astages.infrastructure.capability.AProvider;
import com.alessandro.astages.infrastructure.capability.BlockStage;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

/**
 * Bridges GuGuAddons' machine owner onto AStages' {@code BLOCK_STAGE} data attachment.
 *
 * <p>AStages stores the block owner in its own attachment (set via an EntityPlaceEvent that requires
 * a player) and reads it back for its Jade tooltip. Schematicannon-printed blocks never fire that
 * event, so their AStages owner stays empty. We mirror the owner onto the attachment so Jade shows
 * the cannon placer for every printed machine.
 */
public final class AStagesBlockOwner {
    private AStagesBlockOwner() {
    }

    public static void setBlockOwner(BlockEntity blockEntity, UUID ownerId) {
        if (blockEntity == null || ownerId == null) {
            return;
        }
        BlockStage stage = blockEntity.getData(AProvider.BLOCK_STAGE);
        stage.setOwner(ownerId);
        blockEntity.setChanged();
    }
}
