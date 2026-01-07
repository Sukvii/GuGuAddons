package com.gugucraft.guguaddons.client;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public class QuestInterfaceTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() == ModBlocks.QUEST_INTERFACE_BLOCK.get().asItem()) {
            List<Component> tooltip = event.getToolTip();

            // Stress Impact (Dynamic) - Always Visible
            tooltip.add(com.simibubi.create.foundation.utility.CreateLang.translate("tooltip.stressImpact")
                    .style(ChatFormatting.GRAY).component());

            float impact = 512.0f;
            // High impact
            StressImpact impactId = StressImpact.HIGH;
            ChatFormatting color = impactId.getAbsoluteColor();

            // Progress Bar: [][][]
            String bar = TooltipHelper.makeProgressBar(3, 3);
            MutableComponent barComponent = Component.literal(bar).withStyle(color);

            barComponent.append(Component.literal(" "));

            // Check Goggles
            boolean hasGoggles = event.getEntity() != null && GogglesItem.isWearingGoggles(event.getEntity());

            if (hasGoggles) {
                // Display: 512x RPM
                barComponent.append(Component.literal((int) impact + "x RPM").withStyle(ChatFormatting.GOLD));
            } else {
                // Display: High
                barComponent.append(com.simibubi.create.foundation.utility.CreateLang
                        .translate("tooltip.stressImpact.high").style(color).component());
            }

            tooltip.add(barComponent);
        }
    }
}
