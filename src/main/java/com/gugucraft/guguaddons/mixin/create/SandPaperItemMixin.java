package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.CreateRecipeStageHooks;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(SandPaperItem.class)
public abstract class SandPaperItemMixin {
    @Unique
    private UUID guguaddons$usingPlayer;

    @Inject(method = "use", at = @At("HEAD"))
    private void guguaddons$captureUsePlayer(Level level, Player player, InteractionHand hand,
                                             CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        guguaddons$usingPlayer = player.getUUID();
    }

    @Redirect(method = "use", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/equipment/sandPaper/SandPaperPolishingRecipe;canPolish(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean guguaddons$canPolish(Level level, ItemStack stack) {
        return guguaddons$usingPlayer == null
                ? SandPaperPolishingRecipe.canPolish(level, stack)
                : CreateRecipeStageHooks.canPolish(level, stack, guguaddons$usingPlayer);
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void guguaddons$captureFinishPlayer(ItemStack stack, Level level, LivingEntity entity,
                                                CallbackInfoReturnable<ItemStack> cir) {
        guguaddons$usingPlayer = entity instanceof Player player ? player.getUUID() : null;
    }

    @Redirect(method = "finishUsingItem", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/equipment/sandPaper/SandPaperPolishingRecipe;applyPolish(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack guguaddons$applyPolish(Level level, Vec3 position, ItemStack stack, ItemStack sandPaperStack) {
        return guguaddons$usingPlayer == null
                ? SandPaperPolishingRecipe.applyPolish(level, position, stack, sandPaperStack)
                : CreateRecipeStageHooks.applyPolish(level, position, stack, sandPaperStack, guguaddons$usingPlayer);
    }
}
