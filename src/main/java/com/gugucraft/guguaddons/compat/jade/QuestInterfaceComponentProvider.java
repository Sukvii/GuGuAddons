package com.gugucraft.guguaddons.compat.jade;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.block.entity.QuestInterfaceBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.config.IPluginConfig;
import java.lang.Math;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.TeamData;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;

public enum QuestInterfaceComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof QuestInterfaceBlockEntity be) {
            boolean formed = be.isStructureFormed();
            if (formed) {
                float speed = be.getStructureSpeed();
                float stress = Math.abs(speed) * 32;
                tooltip.add(
                        Component.translatable("jade.guguaddons.stress_consumption", String.format("%.0f", stress)));
            } else {
                tooltip.add(Component.translatable("message.guguaddons.structure_not_formed")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }

            if (formed && be.getLevel() != null && be.getLevel().isClientSide) {
                Task task = be.getTask();
                UUID teamId = be.getTeamId();
                if (task != null && teamId != null) {
                    TeamData data = FTBQuestsAPI.api().getQuestFile(true).getNullableTeamData(teamId);
                    if (data != null) {
                        long progress = data.getProgress(task);
                        long max = task.getMaxProgress();
                        if (max > 0) {
                            float ratio = (float) progress / max;
                            int percent = (int) (ratio * 100);
                            Component text = Component.translatable("jade.guguaddons.task_progress", percent);
                            if (Screen.hasShiftDown()) {
                                IElementHelper helper = IElementHelper.get();
                                // Green gradient: 0xFF28A828 to 0xFF1D7D1D
                                tooltip.add(helper.progress(ratio, text,
                                        helper.progressStyle().color(0xFF28A828, 0xFF1D7D1D).textColor(0xFFFFFFFF),
                                        BoxStyle.getTransparent(), false));
                            } else {
                                tooltip.add(text);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "quest_interface");
    }
}
