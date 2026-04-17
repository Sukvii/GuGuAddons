package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.security.CreatePackageDupeGuard;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RepackagerBlockEntity.class)
public abstract class RepackagerBlockEntityMixin {
    @Inject(method = "attemptToRepackage", at = @At("HEAD"), cancellable = true)
    private void guguaddons$attemptToRepackage(IItemHandler inventory, CallbackInfo ci) {
        CreatePackageDupeGuard.attemptToRepackage((RepackagerBlockEntity) (Object) this, inventory);
        ci.cancel();
    }
}
