package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.LaunchedItemOwnerAccess;
import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LaunchedItem.ForBlockState.class)
public abstract class LaunchedBlockStateItemMixin {
    @Inject(method = "place", at = @At("TAIL"))
    private void guguaddons$applyOwnerToPrintedBlock(Level world, CallbackInfo ci) {
        LaunchedItem.ForBlockState self = (LaunchedItem.ForBlockState) (Object) this;
        UUID owner = ((LaunchedItemOwnerAccess) this).guguaddons$getLaunchedOwner();
        if (owner == null) {
            return;
        }

        BlockEntity blockEntity = world.getBlockEntity(self.target);
        MachineOwnerHelper.setOwner(blockEntity, owner);
    }
}
