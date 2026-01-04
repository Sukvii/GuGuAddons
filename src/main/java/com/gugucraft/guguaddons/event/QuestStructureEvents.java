package com.gugucraft.guguaddons.event;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.block.entity.QuestInterfaceBlockEntity;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbquests.net.BlockConfigRequestMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public class QuestStructureEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            BlockPos pos = event.getPos();
            if (level.getBlockState(pos).is(Blocks.STONE)) {
                // Search for a QuestInterfaceBlockEntity nearby
                // Max radius 3 blocks is sufficient given the 3x3x3 size.
                // The structure center is max 1 block away from stone, controller is max 2 blocks away.
                // Let's check a 5x5x5 area centered on the clicked stone.

                BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
                for (int x = -2; x <= 2; x++) {
                    for (int y = -2; y <= 2; y++) {
                        for (int z = -2; z <= 2; z++) {
                            searchPos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                            BlockEntity be = level.getBlockEntity(searchPos);
                            if (be instanceof QuestInterfaceBlockEntity questInterface) {
                                if (questInterface.isStructureFormed() && questInterface.isBlockInStructure(pos)) {
                                    // Open GUI
                                    NetworkManager.sendToPlayer(player, new BlockConfigRequestMessage(questInterface.getBlockPos(), BlockConfigRequestMessage.BlockType.TASK_SCREEN));
                                    player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                                    event.setCanceled(true);
                                    event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
