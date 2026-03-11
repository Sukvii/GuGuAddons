package com.gugucraft.guguaddons.ponder;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.registry.ModBlocks;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class GuGuAddonsPonderTags {

    public static final ResourceLocation GUGU_ADDONS = ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID,
            "general");

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(GUGU_ADDONS)
                .item(Items.BOOK)
                .title("guguaddons.ponder.tag.general")
                .description("guguaddons.ponder.tag.general.description")
                .addToIndex()
                .register();

        helper.addToTag(GUGU_ADDONS)
                .add(ModBlocks.VACUUM_CHAMBER.getId())
                .add(ModBlocks.CENTRIFUGE.getId());
    }
}
