package com.gugucraft.guguaddons.mixin.arsnouveau;

import com.gugucraft.guguaddons.client.compat.arsnouveau.ScryingMarkers;
import com.hollingsworth.arsnouveau.api.scrying.IScryer;
import net.minecraft.core.Vec3i;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.hollingsworth.arsnouveau.common.event.ScryEvents", remap = false)
public abstract class ScryEventsMixin {
    @Unique
    private static final Vec3i guguaddons$SCRYING_RANGE = new Vec3i(32, 120, 32);

    @Redirect(method = "playerTickEvent", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/scrying/IScryer;getScryingSize()Lnet/minecraft/core/Vec3i;"))
    private static Vec3i guguaddons$expandRange(IScryer scryer) {
        return guguaddons$SCRYING_RANGE;
    }

    @Redirect(method = "playerTickEvent", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/scrying/IScryer;getScryMax()I"))
    private static int guguaddons$expandLimit(IScryer scryer) {
        return 128;
    }

    @Inject(method = "renderScry", at = @At("HEAD"), cancellable = true)
    private static void guguaddons$renderMarkers(RenderLevelStageEvent event, CallbackInfo ci) {
        ScryingMarkers.render(event);
        ci.cancel();
    }
}
