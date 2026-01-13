package com.gugucraft.guguaddons.compat.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class GuGuJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        // Register common providers here
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(QuestInterfaceComponentProvider.INSTANCE,
                com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.class);
        registration.registerBlockComponent(QuestSubmissionComponentProvider.INSTANCE,
                com.gugucraft.guguaddons.block.custom.QuestSubmissionBlock.class);
    }
}
