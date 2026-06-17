package com.gugucraft.guguaddons.mixin;

import com.gugucraft.guguaddons.stage.MachineOwnerAccess;
import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(BlockEntity.class)
public abstract class BlockEntityOwnerMixin implements MachineOwnerAccess {
    @Unique
    private UUID guguaddons$machineOwner;

    @Override
    public UUID guguaddons$getMachineOwner() {
        return guguaddons$machineOwner;
    }

    @Override
    public void guguaddons$setMachineOwner(UUID ownerId) {
        guguaddons$machineOwner = ownerId;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void guguaddons$loadOwner(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        guguaddons$machineOwner = MachineOwnerHelper.getOwner(tag);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void guguaddons$saveOwner(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        MachineOwnerHelper.setOwner(tag, guguaddons$machineOwner);
    }
}
