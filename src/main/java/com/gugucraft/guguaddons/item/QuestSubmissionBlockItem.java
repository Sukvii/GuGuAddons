package com.gugucraft.guguaddons.item;

import com.gugucraft.guguaddons.block.custom.QuestSubmissionBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class QuestSubmissionBlockItem extends BlockItem {
    public QuestSubmissionBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // Check for BlockEntity data
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            int processingSpeed = customData.copyTag().getInt("ProcessingSpeed");
            // If processingSpeed is 0 (missing), it effectively means 1 (default),
            // but let's check if it's actually there to display upgrade info.
            // The default is 1 in the BlockEntity.

            // Only display if effectively > 1 or explicitly upgraded?
            // The user said "Display NBT data on tooltip".
            // If the NBT is present, we show it.
            if (customData.contains("ProcessingSpeed")) {
                // processingSpeed is already defined above
                int tier = 0;
                if (processingSpeed >= 16)
                    tier = 3;
                else if (processingSpeed >= 8)
                    tier = 2;
                else if (processingSpeed >= 4)
                    tier = 1;

                tooltipComponents.add(Component.translatable("tooltip.guguaddons.submission_tier", tier));
            }
        }
    }
}
