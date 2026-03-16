package com.gugucraft.guguaddons.config;

public record ServerConfigSnapshot(
        String slashBackRefillTemplate,
        String slashBackRefillMaterial,
        int ftbChunksClaimPrice,
        double ftbChunksUnclaimRefundRatio,
        int ftbChunksClaimConfirmTimeoutTicks,
        boolean stockEnabled) {

    public ServerConfigSnapshot {
        slashBackRefillTemplate = normalizeItemId(slashBackRefillTemplate, Config.DEFAULT_SLASH_BACK_REFILL_TEMPLATE);
        slashBackRefillMaterial = normalizeItemId(slashBackRefillMaterial, Config.DEFAULT_SLASH_BACK_REFILL_MATERIAL);
        ftbChunksClaimPrice = Math.max(0, ftbChunksClaimPrice);
        ftbChunksUnclaimRefundRatio = Math.max(0.0D, Math.min(1.0D, ftbChunksUnclaimRefundRatio));
        ftbChunksClaimConfirmTimeoutTicks = Math.max(20, ftbChunksClaimConfirmTimeoutTicks);
    }

    private static String normalizeItemId(String itemId, String fallback) {
        if (itemId == null || itemId.isBlank()) {
            return fallback;
        }
        return itemId;
    }
}
