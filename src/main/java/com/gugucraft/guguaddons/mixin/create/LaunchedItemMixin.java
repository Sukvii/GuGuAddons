package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.LaunchedItemOwnerAccess;
import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.schematics.cannon.LaunchedItem;

import java.util.UUID;

@Mixin(LaunchedItem.class)
public abstract class LaunchedItemMixin implements LaunchedItemOwnerAccess {
    @Unique
    @Nullable
    private UUID guguaddons$launchedOwner;

    @Override
    @Nullable
    public UUID guguaddons$getLaunchedOwner() {
        return guguaddons$launchedOwner;
    }

    @Override
    public void guguaddons$setLaunchedOwner(@Nullable UUID ownerId) {
        guguaddons$launchedOwner = ownerId;
    }

    @Inject(method = "serializeNBT", at = @At("RETURN"))
    private void guguaddons$saveLaunchedOwner(HolderLookup.Provider registries,
            CallbackInfoReturnable<CompoundTag> cir) {
        MachineOwnerHelper.setOwner(cir.getReturnValue(), guguaddons$launchedOwner);
    }

    @Inject(method = "readNBT", at = @At("TAIL"))
    private void guguaddons$loadLaunchedOwner(CompoundTag c, HolderLookup.Provider registries,
            HolderGetter<Block> holderGetter, CallbackInfo ci) {
        guguaddons$launchedOwner = MachineOwnerHelper.getOwner(c);
    }
}
