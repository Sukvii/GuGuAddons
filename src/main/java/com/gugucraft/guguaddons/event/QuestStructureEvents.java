package com.gugucraft.guguaddons.event;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.block.entity.QuestInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public class QuestStructureEvents {

    @SubscribeEvent
    public static void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        handleBlockChange(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onBlockPlace(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
        handleBlockChange(event.getLevel(), event.getPos());
    }

    private static void handleBlockChange(net.minecraft.world.level.LevelAccessor levelAccessor, BlockPos pos) {
        if (!(levelAccessor instanceof Level level) || level.isClientSide)
            return;

        QuestInterfaceBlockEntity.requestStructureRefreshAround(level, pos);
    }

    @SubscribeEvent
    public static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            level.getServer().execute(() ->
                    QuestInterfaceBlockEntity.requestStructureRefreshForChunk(level, event.getChunk().getPos()));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            QuestInterfaceBlockEntity.requestStructureRefreshForChunk(level, event.getChunk().getPos());
        }
    }
}
