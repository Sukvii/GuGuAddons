package com.gugucraft.guguaddons.client;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.item.DeathRecallItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public final class DeathRecallTooltipHandler {
    private DeathRecallTooltipHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof DeathRecallItem)) {
            return;
        }

        List<Component> tooltipComponents = event.getToolTip();
        if (Screen.hasShiftDown()) {
            Style createGold = Style.EMPTY.withColor(0xC7954B);

            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.summary")
                    .withStyle(createGold));

            tooltipComponents.add(Component.empty());

            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.condition1")
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.behaviour1")
                    .withStyle(createGold));

            tooltipComponents.add(Component.empty());

            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.condition2")
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.behaviour2")
                    .withStyle(createGold));

            tooltipComponents.add(Component.empty());

            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.condition3")
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.behaviour3")
                    .withStyle(createGold));

            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.empty());

            tooltipComponents.add(Component.translatable("item.guguaddons.slash_back_terminal.tooltip.flavor")
                    .withStyle(ChatFormatting.DARK_PURPLE)
                    .withStyle(ChatFormatting.ITALIC));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.guguaddons.hold_for_description")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        GlobalPos pos = DeathRecallItem.getDeathLocation(event.getItemStack());
        if (pos != null) {
            tooltipComponents
                    .add(Component.translatable("tooltip.guguaddons.recall_location", pos.pos().toShortString())
                            .withStyle(ChatFormatting.GRAY));
            tooltipComponents
                    .add(Component
                            .translatable("tooltip.guguaddons.recall_dimension", pos.dimension().location().toString())
                            .withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(
                    Component.translatable("tooltip.guguaddons.recall_no_location").withStyle(ChatFormatting.GRAY));
        }
    }
}
