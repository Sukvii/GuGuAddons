package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.level.block.Portal;

public class TestPortalBlock extends Block implements Portal {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public TestPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = state.getValue(AXIS);
        if (direction.getAxis() != axis && direction.getAxis().isHorizontal()) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        } else {
            if (direction.getAxis() == axis || direction == Direction.UP || direction == Direction.DOWN) {
                if (neighborState.isAir()) {
                    return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                }
            }
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public DimensionTransition getPortalDestination(ServerLevel serverLevel, Entity entity, BlockPos pos) {
        ResourceKey<Level> destKey = serverLevel.dimension() == ModDimensions.TEST_DIMENSION_KEY
                ? Level.OVERWORLD
                : ModDimensions.TEST_DIMENSION_KEY;

        ServerLevel destLevel = serverLevel.getServer().getLevel(destKey);
        if (destLevel != null) {
            BlockPos currentPos = entity.blockPosition();
            // 1:1 Coordinate mapping
            BlockPos targetPos = new BlockPos(currentPos.getX(), currentPos.getY(), currentPos.getZ());

            // 1. Search for existing portal
            java.util.Optional<BlockPos> existingPortal = destLevel.getPoiManager().find(
                    poiType -> poiType.is(com.gugucraft.guguaddons.registry.ModPOIs.TEST_PORTAL.getKey()),
                    p -> true, // Accept any position
                    targetPos,
                    128, // Search radius
                    net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);

            BlockPos portalPos;
            if (existingPortal.isPresent()) {
                portalPos = existingPortal.get();
            } else {
                // 2. Generate new portal if not found
                portalPos = generatePortal(destLevel, targetPos);
            }

            // Teleport to the portal position
            Vec3 spawnPos = new Vec3(portalPos.getX() + 0.5, portalPos.getY(), portalPos.getZ() + 0.5);

            return new DimensionTransition(
                    destLevel,
                    spawnPos,
                    Vec3.ZERO,
                    entity.getYRot(),
                    entity.getXRot(),
                    DimensionTransition.DO_NOTHING);
        }
        return null;
    }

    private BlockPos generatePortal(ServerLevel level, BlockPos targetPos) {
        // Find a suitable Y level
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                targetPos.getX(), targetPos.getZ());
        // Ensure we are within bounds
        if (y < level.getMinBuildHeight())
            y = level.getMinBuildHeight();
        if (y > level.getMaxBuildHeight() - 10)
            y = level.getMaxBuildHeight() - 10;

        BlockPos basePos = new BlockPos(targetPos.getX(), y + 1, targetPos.getZ());

        // Frame orientation: Axis X (East-West)
        Direction.Axis axis = Direction.Axis.X;

        // Build the frame (4x5)
        // Frame blocks: ModBlocks.TEST_PORTAL_FRAME
        // Portal blocks: ModBlocks.TEST_PORTAL

        BlockState frameState = com.gugucraft.guguaddons.registry.ModBlocks.TEST_PORTAL_FRAME.get().defaultBlockState();
        BlockState portalState = com.gugucraft.guguaddons.registry.ModBlocks.TEST_PORTAL.get().defaultBlockState()
                .setValue(TestPortalBlock.AXIS, axis);

        // Clear area and build
        for (int x = -1; x <= 2; x++) {
            for (int h = 0; h < 5; h++) {
                for (int z = -1; z <= 1; z++) { // Clear a bit of width
                    BlockPos p = basePos.offset(x, h, z);
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Build Frame
        // Bottom & Top
        for (int i = 0; i < 2; i++) {
            level.setBlock(basePos.offset(i, 0, 0), frameState, 3); // Bottom
            level.setBlock(basePos.offset(i, 4, 0), frameState, 3); // Top
        }
        // Sides
        for (int h = 0; h < 5; h++) {
            level.setBlock(basePos.offset(-1, h, 0), frameState, 3); // Left
            level.setBlock(basePos.offset(2, h, 0), frameState, 3); // Right
        }

        // Fill Portal
        for (int i = 0; i < 2; i++) {
            for (int h = 1; h < 4; h++) {
                level.setBlock(basePos.offset(i, h, 0), portalState, 3);
            }
        }

        // Return a position inside the portal
        return basePos.offset(0, 1, 0);
    }
}
