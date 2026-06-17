package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.LaunchedItemOwnerAccess;
import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LaunchedItem.ForBelt.class)
public abstract class LaunchedBeltItemMixin {
    @Inject(method = "place", at = @At("TAIL"))
    private void guguaddons$copyOwnerToPrintedBelt(Level world, CallbackInfo ci) {
        LaunchedItem.ForBelt belt = (LaunchedItem.ForBelt) (Object) this;
        UUID owner = ((LaunchedItemOwnerAccess) this).guguaddons$getLaunchedOwner();
        if (owner == null) {
            return;
        }

        BlockState state = belt.state;
        boolean isStart = state.getValue(BeltBlock.PART) == BeltPart.START;
        BlockPos offset = BeltBlock.nextSegmentPosition(state, BlockPos.ZERO, isStart);
        for (int segment = 0; segment < belt.length; segment++) {
            BlockPos segmentPos = belt.target.offset(offset.getX() * segment, offset.getY() * segment,
                    offset.getZ() * segment);
            BlockEntity blockEntity = world.getBlockEntity(segmentPos);
            MachineOwnerHelper.setOwner(blockEntity, owner);
        }
    }
}
