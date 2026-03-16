package com.gugucraft.guguaddons;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
        public static final String DEFAULT_SLASH_BACK_REFILL_TEMPLATE = "minecraft:netherite_upgrade_smithing_template";
        public static final String DEFAULT_SLASH_BACK_REFILL_MATERIAL = "minecraft:ender_pearl";
        public static final int DEFAULT_FTB_CHUNKS_CLAIM_PRICE = 100;
        public static final double DEFAULT_FTB_CHUNKS_UNCLAIM_REFUND_RATIO = 0.5D;
        public static final int DEFAULT_FTB_CHUNKS_CLAIM_CONFIRM_TIMEOUT_TICKS = 200;
        public static final boolean DEFAULT_STOCK_ENABLED = false;

        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        private static final Map<String, String> INVALID_ITEM_CONFIG_WARNINGS = new ConcurrentHashMap<>();

        public static final ModConfigSpec.ConfigValue<String> SLASH_BACK_REFILL_TEMPLATE;
        public static final ModConfigSpec.ConfigValue<String> SLASH_BACK_REFILL_MATERIAL;
        public static final ModConfigSpec.IntValue FTB_CHUNKS_CLAIM_PRICE;
        public static final ModConfigSpec.DoubleValue FTB_CHUNKS_UNCLAIM_REFUND_RATIO;
        public static final ModConfigSpec.IntValue FTB_CHUNKS_CLAIM_CONFIRM_TIMEOUT_TICKS;
        public static final ModConfigSpec.BooleanValue STOCK_ENABLED;

        static {
                SLASH_BACK_REFILL_TEMPLATE = BUILDER
                                .comment("The item ID used in the Smithing Table template slot to repair the Slash Back Terminal.")
                                .define("slashBackRefillTemplate", DEFAULT_SLASH_BACK_REFILL_TEMPLATE, Config::isValidConfiguredItemId);

                SLASH_BACK_REFILL_MATERIAL = BUILDER
                                .comment("The item ID used in the Smithing Table material slot (addition) to repair the Slash Back Terminal.")
                                .define("slashBackRefillMaterial", DEFAULT_SLASH_BACK_REFILL_MATERIAL, Config::isValidConfiguredItemId);

                FTB_CHUNKS_CLAIM_PRICE = BUILDER
                                .comment("Numismatics cost (sp) per chunk claim in FTB Chunks. Set to 0 to disable charging.")
                                .defineInRange("ftbChunksClaimPrice", DEFAULT_FTB_CHUNKS_CLAIM_PRICE, 0, Integer.MAX_VALUE);

                FTB_CHUNKS_UNCLAIM_REFUND_RATIO = BUILDER
                                .comment("Refund ratio on unclaim, based on the paid claim price. 0.0 = no refund, 1.0 = full refund.")
                                .defineInRange("ftbChunksUnclaimRefundRatio", DEFAULT_FTB_CHUNKS_UNCLAIM_REFUND_RATIO, 0.0D, 1.0D);

                FTB_CHUNKS_CLAIM_CONFIRM_TIMEOUT_TICKS = BUILDER
                                .comment("Ticks before a pending chunk-claim confirmation expires.")
                                .defineInRange("ftbChunksClaimConfirmTimeoutTicks", DEFAULT_FTB_CHUNKS_CLAIM_CONFIRM_TIMEOUT_TICKS, 20, 20 * 60 * 10);

                STOCK_ENABLED = BUILDER
                                .comment("Whether the stock market system is enabled. Disabled by default and synced from the server to clients.")
                                .define("stockEnabled", DEFAULT_STOCK_ENABLED);
        }

        static final ModConfigSpec SPEC = BUILDER.build();

        public static ServerConfigSnapshot captureConfiguredSnapshot() {
                return new ServerConfigSnapshot(
                                SLASH_BACK_REFILL_TEMPLATE.get(),
                                SLASH_BACK_REFILL_MATERIAL.get(),
                                FTB_CHUNKS_CLAIM_PRICE.get(),
                                FTB_CHUNKS_UNCLAIM_REFUND_RATIO.get(),
                                FTB_CHUNKS_CLAIM_CONFIRM_TIMEOUT_TICKS.get(),
                                STOCK_ENABLED.get());
        }

        public static ServerConfigSnapshot getEffectiveSnapshot() {
                return ConfigSyncState.getEffectiveSnapshot();
        }

        public static Item getConfiguredSlashBackRefillTemplateItem() {
                return resolveConfiguredItem("slashBackRefillTemplate", captureConfiguredSnapshot().slashBackRefillTemplate(),
                                DEFAULT_SLASH_BACK_REFILL_TEMPLATE);
        }

        public static Item getConfiguredSlashBackRefillMaterialItem() {
                return resolveConfiguredItem("slashBackRefillMaterial", captureConfiguredSnapshot().slashBackRefillMaterial(),
                                DEFAULT_SLASH_BACK_REFILL_MATERIAL);
        }

        public static Item getEffectiveSlashBackRefillTemplateItem() {
                return resolveConfiguredItem("slashBackRefillTemplate", getEffectiveSnapshot().slashBackRefillTemplate(),
                                DEFAULT_SLASH_BACK_REFILL_TEMPLATE);
        }

        public static Item getEffectiveSlashBackRefillMaterialItem() {
                return resolveConfiguredItem("slashBackRefillMaterial", getEffectiveSnapshot().slashBackRefillMaterial(),
                                DEFAULT_SLASH_BACK_REFILL_MATERIAL);
        }

        public static int getConfiguredFtbChunksClaimPrice() {
                return Math.max(0, FTB_CHUNKS_CLAIM_PRICE.get());
        }

        public static double getConfiguredFtbChunksUnclaimRefundRatio() {
                return Math.max(0.0D, Math.min(1.0D, FTB_CHUNKS_UNCLAIM_REFUND_RATIO.get()));
        }

        public static int getConfiguredFtbChunksClaimConfirmTimeoutTicks() {
                return Math.max(20, FTB_CHUNKS_CLAIM_CONFIRM_TIMEOUT_TICKS.get());
        }

        public static boolean isConfiguredStockEnabled() {
                return STOCK_ENABLED.get();
        }

        public static boolean isEffectiveStockEnabled() {
                return getEffectiveSnapshot().stockEnabled();
        }

        private static Item resolveConfiguredItem(String configKey, String configuredId, String defaultId) {
                Item configuredItem = resolveRegisteredItem(configuredId);
                if (configuredItem != null && configuredItem != Items.AIR) {
                        return configuredItem;
                }

                logInvalidConfiguredItem(configKey, configuredId, defaultId);

                Item fallbackItem = resolveRegisteredItem(defaultId);
                return fallbackItem != null && fallbackItem != Items.AIR ? fallbackItem : Items.AIR;
        }

        @Nullable
        private static Item resolveRegisteredItem(String itemId) {
                ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
                if (resourceLocation == null) {
                        return null;
                }
                return BuiltInRegistries.ITEM.getOptional(resourceLocation).orElse(null);
        }

        private static void logInvalidConfiguredItem(String configKey, String configuredId, String defaultId) {
                String previousWarning = INVALID_ITEM_CONFIG_WARNINGS.put(configKey, configuredId);
                if (configuredId.equals(previousWarning)) {
                        return;
                }

                GuGuAddons.LOGGER.warn("Invalid item id '{}' for config '{}', falling back to '{}'.",
                                configuredId, configKey, defaultId);
        }

        private static boolean isValidConfiguredItemId(Object value) {
                return value instanceof String itemId && resolveRegisteredItem(itemId) != null;
        }
}
