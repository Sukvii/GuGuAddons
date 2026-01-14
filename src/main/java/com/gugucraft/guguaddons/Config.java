package com.gugucraft.guguaddons;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.ConfigValue<String> SLASH_BACK_REFILL_TEMPLATE;
        public static final ModConfigSpec.ConfigValue<String> SLASH_BACK_REFILL_MATERIAL;

        static {
                SLASH_BACK_REFILL_TEMPLATE = BUILDER
                                .comment("The item ID used in the Smithing Table template slot to repair the Slash Back Terminal.")
                                .define("slashBackRefillTemplate", "minecraft:netherite_upgrade_smithing_template");

                SLASH_BACK_REFILL_MATERIAL = BUILDER
                                .comment("The item ID used in the Smithing Table material slot (addition) to repair the Slash Back Terminal.")
                                .define("slashBackRefillMaterial", "minecraft:ender_pearl");
        }

        static final ModConfigSpec SPEC = BUILDER.build();
}
