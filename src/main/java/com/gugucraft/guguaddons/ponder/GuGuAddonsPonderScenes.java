package com.gugucraft.guguaddons.ponder;

import com.gugucraft.guguaddons.ponder.scenes.QuestInterfaceScenes;
import com.gugucraft.guguaddons.registry.ModBlocks;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class GuGuAddonsPonderScenes {

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ModBlocks.QUEST_INTERFACE_BLOCK.getId())
                .addStoryBoard("quest_interface/construction", QuestInterfaceScenes::buildStructure,
                        GuGuAddonsPonderTags.GUGU_ADDONS);
    }
}
