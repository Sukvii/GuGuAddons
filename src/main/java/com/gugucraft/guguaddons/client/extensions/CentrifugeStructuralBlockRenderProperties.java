package com.gugucraft.guguaddons.client.extensions;

import com.gugucraft.guguaddons.block.custom.CentrifugeStructuralBlock;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class CentrifugeStructuralBlockRenderProperties implements IClientBlockExtensions, MultiPosDestructionHandler {
    @Override
    public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
        if (target instanceof BlockHitResult blockHitResult
                && state.getBlock() instanceof CentrifugeStructuralBlock structuralBlock
                && structuralBlock.stillValid(level, blockHitResult.getBlockPos(), state, false)) {
            manager.crack(CentrifugeStructuralBlock.getMaster(level, blockHitResult.getBlockPos(), state),
                    blockHitResult.getDirection());
            return true;
        }

        return IClientBlockExtensions.super.addHitEffects(state, level, target, manager);
    }

    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        if (!(level instanceof ClientLevel clientLevel)
                || !(state.getBlock() instanceof CentrifugeStructuralBlock structuralBlock)) {
            return true;
        }

        BlockState particleState = structuralBlock.resolveParticleState(level, pos, state);
        VoxelShape voxelShape = state.getShape(clientLevel, pos);
        MutableInt boxCount = new MutableInt(0);
        voxelShape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> boxCount.increment());

        if (boxCount.intValue() == 0 || particleState.isAir()) {
            return true;
        }

        double chance = 1d / boxCount.intValue();
        voxelShape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            double width = x2 - x1;
            double height = y2 - y1;
            double length = z2 - z1;
            int xParts = Math.max(2, Mth.ceil(Math.min(1, width) * 4));
            int yParts = Math.max(2, Mth.ceil(Math.min(1, height) * 4));
            int zParts = Math.max(2, Mth.ceil(Math.min(1, length) * 4));

            for (int xIndex = 0; xIndex < xParts; xIndex++) {
                for (int yIndex = 0; yIndex < yParts; yIndex++) {
                    for (int zIndex = 0; zIndex < zParts; zIndex++) {
                        if (clientLevel.random.nextDouble() > chance) {
                            continue;
                        }

                        double localX = (xIndex + .5d) / xParts;
                        double localY = (yIndex + .5d) / yParts;
                        double localZ = (zIndex + .5d) / zParts;
                        double worldX = pos.getX() + localX * width + x1;
                        double worldY = pos.getY() + localY * height + y1;
                        double worldZ = pos.getZ() + localZ * length + z1;

                        manager.add(new TerrainParticle(clientLevel, worldX, worldY, worldZ,
                                localX - .5d, localY - .5d, localZ - .5d, particleState, pos)
                                .updateSprite(particleState, pos));
                    }
                }
            }
        });

        return true;
    }

    @Override
    public @Nullable Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState,
            int progress) {
        if (!(blockState.getBlock() instanceof CentrifugeStructuralBlock structuralBlock)
                || !structuralBlock.stillValid(level, pos, blockState, false)) {
            return null;
        }

        HashSet<BlockPos> positions = new HashSet<>();
        positions.add(CentrifugeStructuralBlock.getMaster(level, pos, blockState));
        return positions;
    }
}
