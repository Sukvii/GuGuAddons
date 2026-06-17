package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.LaunchedItemOwnerAccess;
import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(SchematicannonBlockEntity.class)
public abstract class SchematicannonBlockEntityMixin {
    @Shadow
    public List<LaunchedItem> flyingBlocks;

    @Inject(method = "launchBlock", at = @At("TAIL"))
    private void guguaddons$stampOwnerOnLaunchedBlock(BlockPos target, ItemStack stack, BlockState state,
            @Nullable CompoundTag data, CallbackInfo ci) {
        guguaddons$stampOwnerOnLastLaunched();
    }

    @Inject(method = "launchBelt", at = @At("TAIL"))
    private void guguaddons$stampOwnerOnLaunchedBelt(BlockPos target, BlockState state, int length,
            CasingType[] casings, CallbackInfo ci) {
        guguaddons$stampOwnerOnLastLaunched();
    }

    @Unique
    private void guguaddons$stampOwnerOnLastLaunched() {
        UUID owner = MachineOwnerHelper.getOwner((BlockEntity) (Object) this);
        if (owner == null || flyingBlocks.isEmpty()) {
            return;
        }

        LaunchedItem launched = flyingBlocks.get(flyingBlocks.size() - 1);
        if (launched instanceof LaunchedItemOwnerAccess access) {
            access.guguaddons$setLaunchedOwner(owner);
        }
    }
}
