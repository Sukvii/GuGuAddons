package com.gugucraft.guguaddons.mixin.create;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlock;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackagerBlockEntity.class)
public abstract class PackagerBlockEntityMixin {
    @Inject(method = "supportsBlockEntity", at = @At("HEAD"), cancellable = true)
    private void guguaddons$allowEmptyTableClothTarget(BlockEntity target,
            CallbackInfoReturnable<Boolean> cir) {
        if (target != null)
            return;

        PackagerBlockEntity packager = (PackagerBlockEntity) (Object) this;
        Level level = packager.getLevel();
        if (level == null || packager.targetInventory == null)
            return;

        BlockFace targetFace = packager.targetInventory.getTarget().getOpposite();
        BlockState targetState = level.getBlockState(targetFace.getPos());
        if (targetState.getBlock() instanceof TableClothBlock)
            cir.setReturnValue(true);
    }
}
