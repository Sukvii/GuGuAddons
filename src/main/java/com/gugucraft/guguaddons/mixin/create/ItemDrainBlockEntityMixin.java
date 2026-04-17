package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.CreateRecipeStageHooks;
import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemDrainBlockEntity.class)
public abstract class ItemDrainBlockEntityMixin {
    @Redirect(method = { "tryInsertingFromSide", "tick", "continueProcessing" }, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/transfer/GenericItemEmptying;canItemBeEmptied(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean guguaddons$canItemBeEmptied(Level level, ItemStack stack) {
        return CreateRecipeStageHooks.canItemBeEmptied(level, stack,
                MachineOwnerHelper.getOwner((BlockEntity) (Object) this));
    }
}
