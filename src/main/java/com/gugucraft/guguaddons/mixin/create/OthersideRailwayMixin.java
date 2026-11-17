package com.gugucraft.guguaddons.mixin.create;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.content.trains.entity.Carriage$DimensionalCarriageEntity", remap = false)
public abstract class OthersideRailwayMixin {
    @Inject(method = "dismountPlayer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/Integer;Z)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/advancement/CreateAdvancement;awardTo(Lnet/minecraft/world/entity/player/Player;)V",
                    shift = At.Shift.AFTER))
    private void guguaddons$awardOthersideJourney(ServerLevel origin, ServerPlayer player, Integer seat,
                                                boolean capture, CallbackInfo ci) {
        ResourceLocation otherside = ResourceLocation.fromNamespaceAndPath("deeperdarker", "otherside");
        if (!((origin.dimension() == Level.OVERWORLD && player.serverLevel().dimension().location().equals(otherside))
                || (origin.dimension().location().equals(otherside) && player.serverLevel().dimension() == Level.OVERWORLD))) {
            return;
        }
        var advancement = origin.getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath("guguaddons", "otherside_railway"));
        player.getAdvancements().award(advancement, "ride_through_portal");
    }
}
