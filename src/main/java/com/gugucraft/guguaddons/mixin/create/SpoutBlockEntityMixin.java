package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.CreateRecipeStageHooks;
import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpoutBlockEntity.class)
public abstract class SpoutBlockEntityMixin {
    @Redirect(method = { "onItemReceived", "whenItemHeld" }, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;canItemBeFilled(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean guguaddons$canItemBeFilled(Level level, ItemStack stack) {
        return CreateRecipeStageHooks.canItemBeFilled(level, stack,
                MachineOwnerHelper.getOwner((BlockEntity) (Object) this));
    }
}
