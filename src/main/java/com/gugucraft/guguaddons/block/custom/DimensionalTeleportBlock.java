package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.registry.ModDimensions;
import com.gugucraft.guguaddons.world.gen.GuGuDungeonChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ChunkPos;

public class DimensionalTeleportBlock extends Block {
    public DimensionalTeleportBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide) {
            if (level instanceof ServerLevel serverLevel) {
                if (level.dimension() == ModDimensions.TEST_DIMENSION_KEY) {
                    ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
                    if (overworld != null) {
                        BlockPos initialSpawn = overworld.getSharedSpawnPos();
                        BlockPos respawnPos = initialSpawn;

                        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            BlockPos playerRespawn = serverPlayer.getRespawnPosition();
                            if (playerRespawn != null && serverPlayer.getRespawnDimension() == Level.OVERWORLD) {
                                respawnPos = playerRespawn;
                            }
                        }

                        player.teleportTo(overworld, respawnPos.getX() + 0.5, respawnPos.getY() + 1,
                                respawnPos.getZ() + 0.5, java.util.Collections.emptySet(), player.getYRot(),
                                player.getXRot());
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    ServerLevel targetDimension = serverLevel.getServer().getLevel(ModDimensions.TEST_DIMENSION_KEY);
                    if (targetDimension != null) {
                        // Find nearest teleport room
                        BlockPos targetPos = findNearestTeleportRoom(targetDimension, player.blockPosition());

                        if (targetPos != null) {
                            player.teleportTo(targetDimension, targetPos.getX() + 0.5, targetPos.getY() + 1,
                                    targetPos.getZ() + 0.5, java.util.Collections.emptySet(), player.getYRot(),
                                    player.getXRot());
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    private BlockPos findNearestTeleportRoom(ServerLevel level, BlockPos origin) {
        // We use seed 0 to match generation logic which doesn't have access to world
        // seed easily
        long seed = 0;
        ChunkPos originChunk = new ChunkPos(origin);

        // Helix/Spiral search for nearest chunk that satisfies the condition
        int radius = 0;
        int maxRadius = 100; // Don't search forever

        while (radius < maxRadius) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Check perimeter only to avoid re-checking inner squares
                    if (Math.abs(x) != radius && Math.abs(z) != radius)
                        continue;

                    int checkX = originChunk.x + x;
                    int checkZ = originChunk.z + z;

                    if (GuGuDungeonChunkGenerator.isTeleportRoom(seed, checkX, checkZ)) {
                        // Found it! Return center of this chunk, Y=2 (assuming floor is at 0/1)
                        return new BlockPos(checkX * 16 + 8, 2, checkZ * 16 + 8);
                    }
                }
            }
            radius++;
        }

        // Fallback to 0,0 if nothing found (should rarely happen given 1/20 chance)
        return new BlockPos(0, 5, 0);
    }
}
