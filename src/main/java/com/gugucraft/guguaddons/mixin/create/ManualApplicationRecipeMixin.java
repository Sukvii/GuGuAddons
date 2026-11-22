package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mixin(ManualApplicationRecipe.class)
public abstract class ManualApplicationRecipeMixin {
    @Inject(method = "manualApplicationRecipesApplyInWorld", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void guguaddons$beforeMachineUpgrade(RightClickBlock event, CallbackInfo ci,
            @Local Optional<RecipeHolder<?>> foundRecipe, @Share("owner") LocalRef<UUID> owner,
            @Share("fluids") LocalRef<CompoundTag> fluids) {
        var recipe = foundRecipe.orElseThrow();
        var id = recipe.id();
        if (!id.getNamespace().equals(GuGuAddons.MODID) || !(id.getPath().startsWith("item_application/arcane_")
                || id.getPath().startsWith("item_application/resonant_"))) return;
        var machine = event.getLevel().getBlockEntity(event.getPos());
        // The Arcane Casing itself is made by applying a sheet to a log, not by upgrading a machine.
        if (machine == null || id.getPath().equals("item_application/arcane_casing")) return;
        if (!MachineRecipeStageManager.canProcess(machine, recipe)) {
            ci.cancel();
            return;
        }

        owner.set(MachineOwnerHelper.getOwner(machine));
        if (id.getPath().endsWith("_basin") || id.getPath().endsWith("_spout")) {
            var data = machine.saveWithoutMetadata(event.getLevel().registryAccess());
            // Create already drops solid inventories; CMM initializes capacity from the new tier.
            data.getAllKeys().retainAll(Set.of("Tanks", "InputTanks", "OutputTanks", "FluidOverflow"));
            fluids.set(data);
        }
    }

    @Inject(method = "manualApplicationRecipesApplyInWorld", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void guguaddons$restoreMachineUpgrade(RightClickBlock event, CallbackInfo ci,
            @Share("owner") LocalRef<UUID> owner, @Share("fluids") LocalRef<CompoundTag> fluids) {
        if (owner.get() == null && fluids.get() == null) return;
        var level = event.getLevel();
        var machine = level.getBlockEntity(event.getPos());
        if (fluids.get() != null) {
            var registries = level.registryAccess();
            machine.loadWithComponents(machine.saveWithoutMetadata(registries).merge(fluids.get()), registries);
        }
        MachineOwnerHelper.setOwner(machine, owner.get());
        machine.setChanged();
        level.sendBlockUpdated(event.getPos(), machine.getBlockState(), machine.getBlockState(), 2);
    }
}
