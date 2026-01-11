package com.gugucraft.guguaddons.ponder;

import com.gugucraft.guguaddons.GuGuAddons;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class GuGuAddonsPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return GuGuAddons.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        GuGuAddonsPonderScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        GuGuAddonsPonderTags.register(helper);
    }

}
