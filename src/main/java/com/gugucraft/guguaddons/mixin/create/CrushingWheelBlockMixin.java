package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(CrushingWheelBlock.class)
public abstract class CrushingWheelBlockMixin {
    @Inject(method = "updateControllers", at = @At("RETURN"))
    private void guguaddons$copyOwnerToController(BlockState state, Level level, BlockPos pos,
                                                  Direction direction, CallbackInfo ci) {
        BlockEntity firstWheel = level.getBlockEntity(pos);
        BlockEntity secondWheel = level.getBlockEntity(pos.relative(direction, 2));
        UUID owner = MachineOwnerHelper.getOwner(firstWheel);
        if (owner == null) {
            owner = MachineOwnerHelper.getOwner(secondWheel);
        }
        if (owner != null) {
            MachineOwnerHelper.setOwner(level.getBlockEntity(pos.relative(direction)), owner);
        }
    }
}
