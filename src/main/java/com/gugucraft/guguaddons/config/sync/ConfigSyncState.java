package com.gugucraft.guguaddons.config.sync;

import com.gugucraft.guguaddons.config.Config;
import com.gugucraft.guguaddons.config.ServerConfigSnapshot;
import org.jetbrains.annotations.Nullable;

public final class ConfigSyncState {
    @Nullable
    private static volatile ServerConfigSnapshot serverSnapshot;

    private ConfigSyncState() {
    }

    public static ServerConfigSnapshot getEffectiveSnapshot() {
        ServerConfigSnapshot snapshot = serverSnapshot;
        return snapshot != null ? snapshot : getClientFallbackSnapshot();
    }

    public static void applyServerSnapshot(ServerConfigSnapshot snapshot) {
        serverSnapshot = snapshot;
    }

    public static void clearServerSnapshot() {
        serverSnapshot = null;
    }

    private static ServerConfigSnapshot getClientFallbackSnapshot() {
        try {
            return Config.captureConfiguredSnapshot();
        } catch (IllegalStateException ignored) {
            return new ServerConfigSnapshot(
                    Config.DEFAULT_SLASH_BACK_REFILL_TEMPLATE,
                    Config.DEFAULT_SLASH_BACK_REFILL_MATERIAL,
                    Config.DEFAULT_FTB_CHUNKS_CLAIM_PRICE,
                    Config.DEFAULT_FTB_CHUNKS_UNCLAIM_REFUND_RATIO,
                    Config.DEFAULT_FTB_CHUNKS_CLAIM_CONFIRM_TIMEOUT_TICKS,
                    Config.DEFAULT_STOCK_ENABLED);
        }
    }
}
