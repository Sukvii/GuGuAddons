package com.gugucraft.guguaddons.stage;

import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public final class MachineOwnerEvents {
    private MachineOwnerEvents() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiPlaceEvent) {
            for (BlockSnapshot snapshot : multiPlaceEvent.getReplacedBlockSnapshots()) {
                setOwner(level.getBlockEntity(snapshot.getPos()), player);
            }
            return;
        }

        setOwner(level.getBlockEntity(event.getPos()), player);
    }

    private static void setOwner(BlockEntity blockEntity, ServerPlayer player) {
        if (blockEntity != null) {
            MachineOwnerHelper.setOwner(blockEntity, player.getUUID());
        }
    }
}
