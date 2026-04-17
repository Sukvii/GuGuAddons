package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.CreateRecipeStageHooks;
import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(AirCurrent.class)
public abstract class AirCurrentMixin {
    @Shadow
    @Final
    public IAirCurrentSource source;

    @Redirect(method = "tickAffectedEntities", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessing;applyProcessing(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z"))
    private boolean guguaddons$applyEntityProcessing(ItemEntity entity, FanProcessingType type) {
        if (!CreateRecipeStageHooks.canFanProcess(entity.level(), entity.getItem(), type, guguaddons$getOwner())) {
            return false;
        }
        return FanProcessing.applyProcessing(entity, type);
    }

    @Redirect(method = "lambda$tickAffectedHandlers$0", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessing;applyProcessing(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour$TransportedResult;"))
    private TransportedItemStackHandlerBehaviour.TransportedResult guguaddons$applyTransportedProcessing(
            TransportedItemStack transported, Level level, FanProcessingType type) {
        if (!CreateRecipeStageHooks.canFanProcess(level, transported.stack, type, guguaddons$getOwner())) {
            return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
        }
        return FanProcessing.applyProcessing(transported, level, type);
    }

    @Unique
    private UUID guguaddons$getOwner() {
        if (source instanceof BlockEntity blockEntity) {
            return MachineOwnerHelper.getOwner(blockEntity);
        }
        return null;
    }
}
