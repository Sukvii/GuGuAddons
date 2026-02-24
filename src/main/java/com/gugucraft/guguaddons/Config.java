package com.gugucraft.guguaddons;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.ConfigValue<String> SLASH_BACK_REFILL_TEMPLATE;
        public static final ModConfigSpec.ConfigValue<String> SLASH_BACK_REFILL_MATERIAL;
        public static final ModConfigSpec.IntValue FTB_CHUNKS_CLAIM_PRICE;
        public static final ModConfigSpec.DoubleValue FTB_CHUNKS_UNCLAIM_REFUND_RATIO;
        public static final ModConfigSpec.IntValue FTB_CHUNKS_CLAIM_CONFIRM_TIMEOUT_TICKS;

        static {
                SLASH_BACK_REFILL_TEMPLATE = BUILDER
                                .comment("The item ID used in the Smithing Table template slot to repair the Slash Back Terminal.")
                                .define("slashBackRefillTemplate", "minecraft:netherite_upgrade_smithing_template");

                SLASH_BACK_REFILL_MATERIAL = BUILDER
                                .comment("The item ID used in the Smithing Table material slot (addition) to repair the Slash Back Terminal.")
                                .define("slashBackRefillMaterial", "minecraft:ender_pearl");

                FTB_CHUNKS_CLAIM_PRICE = BUILDER
                                .comment("Numismatics cost (sp) per chunk claim in FTB Chunks. Set to 0 to disable charging.")
                                .defineInRange("ftbChunksClaimPrice", 100, 0, Integer.MAX_VALUE);

                FTB_CHUNKS_UNCLAIM_REFUND_RATIO = BUILDER
                                .comment("Refund ratio on unclaim, based on the paid claim price. 0.0 = no refund, 1.0 = full refund.")
                                .defineInRange("ftbChunksUnclaimRefundRatio", 0.5D, 0.0D, 1.0D);

                FTB_CHUNKS_CLAIM_CONFIRM_TIMEOUT_TICKS = BUILDER
                                .comment("Ticks before a pending chunk-claim confirmation expires.")
                                .defineInRange("ftbChunksClaimConfirmTimeoutTicks", 200, 20, 20 * 60 * 10);
        }

        static final ModConfigSpec SPEC = BUILDER.build();
}
