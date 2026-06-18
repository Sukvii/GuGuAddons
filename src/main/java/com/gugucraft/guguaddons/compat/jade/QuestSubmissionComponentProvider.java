package com.gugucraft.guguaddons.compat.jade;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.block.entity.QuestSubmissionBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum QuestSubmissionComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof QuestSubmissionBlockEntity be) {
            int processingSpeed = Math.max(1, be.getProcessingSpeed());
            int cooldown = Math.max(1, 20 / processingSpeed);
            int submissionsPerSecond = 20 / cooldown;
            tooltip.add(Component.translatable("jade.guguaddons.submission_speed", submissionsPerSecond));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "quest_submission");
    }
}
