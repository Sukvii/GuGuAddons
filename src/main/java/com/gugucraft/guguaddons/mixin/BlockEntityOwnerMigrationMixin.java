package com.gugucraft.guguaddons.mixin;

import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One-way migration for worlds that stored machine owners in the old GuGuAddons root NBT field.
 * New saves use AStages' serializable block-owner attachment exclusively.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityOwnerMigrationMixin {
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void guguaddons$migrateLegacyOwner(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        MachineOwnerHelper.migrateLegacyOwner((BlockEntity) (Object) this, tag);
    }
}
