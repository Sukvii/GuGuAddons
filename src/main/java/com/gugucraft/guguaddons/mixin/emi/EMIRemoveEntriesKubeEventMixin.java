package com.gugucraft.guguaddons.mixin.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.latvian.mods.kubejs.recipe.viewer.RecipeViewerEntryType;
import dev.latvian.mods.rhino.Context;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.integration.emi.EMIRemoveEntriesKubeEvent", remap = false)
public abstract class EMIRemoveEntriesKubeEventMixin {
    @Shadow
    @Final
    private RecipeViewerEntryType type;

    @Shadow
    @Final
    private EmiRegistry registry;

    @Unique
    private final Set<Item> guguaddons$hiddenItems = new HashSet<>();

    @Unique
    private boolean guguaddons$itemFilterRegistered;

    @Inject(method = "remove(Ldev/latvian/mods/rhino/Context;Ljava/lang/Object;)V", at = @At("HEAD"), cancellable = true)
    private void guguaddons$batchItemRemovals(Context cx, Object filter, CallbackInfo ci) {
        if (type != RecipeViewerEntryType.ITEM) {
            return;
        }
        Object predicate = type.wrapPredicate(cx, filter);
        if (!(predicate instanceof Ingredient ingredient) || !ingredient.isSimple()) {
            return;
        }

        // Plain item rules share one lookup instead of checking every rule for every stack.
        for (ItemStack stack : ingredient.getItems()) {
            guguaddons$hiddenItems.add(stack.getItem());
        }
        if (!guguaddons$itemFilterRegistered) {
            registry.removeEmiStacks(stack -> {
                ItemStack itemStack = stack.getItemStack();
                return !itemStack.isEmpty() && guguaddons$hiddenItems.contains(itemStack.getItem());
            });
            guguaddons$itemFilterRegistered = true;
        }
        ci.cancel();
    }
}
