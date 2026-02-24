package com.gugucraft.guguaddons.client.ftbchunks;

import java.util.Locale;

import com.gugucraft.guguaddons.ftbchunks.ChunkClaimDecisionC2SPayload;
import com.gugucraft.guguaddons.ftbchunks.ChunkClaimPromptS2CPayload;
import com.gugucraft.guguaddons.ftbchunks.ChunkClaimToastS2CPayload;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ChunkClaimEconomyClientHooks {
    private ChunkClaimEconomyClientHooks() {
    }

    public static void openClaimPrompt(ChunkClaimPromptS2CPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ScreenWrapper wrapper)) {
            PacketDistributor.sendToServer(new ChunkClaimDecisionC2SPayload(payload.sessionId(), false));
            return;
        }

        BaseScreen gui = wrapper.getGui();
        if (!(gui instanceof ChunkScreen)) {
            PacketDistributor.sendToServer(new ChunkClaimDecisionC2SPayload(payload.sessionId(), false));
            return;
        }

        Component title = Component.translatable("menu.guguaddons.chunk_claim_confirm.title");
        Component description = Component.translatable(
                "menu.guguaddons.chunk_claim_confirm.summary",
                payload.chunkCount(),
                formatSpurs(payload.totalCost()));

        gui.openYesNoFull(title, description,
                confirmed -> PacketDistributor
                        .sendToServer(new ChunkClaimDecisionC2SPayload(payload.sessionId(), confirmed)));
    }

    private static String formatSpurs(int amount) {
        return String.format(Locale.ROOT, "%,d sp", Math.max(0, amount));
    }

    public static void showToast(ChunkClaimToastS2CPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Component message = switch (payload.argCount()) {
            case 1 -> Component.translatable(payload.messageKey(), payload.arg1());
            case 2 -> Component.translatable(payload.messageKey(), payload.arg1(), payload.arg2());
            case 3 -> Component.translatable(payload.messageKey(), payload.arg1(), payload.arg2(), payload.arg3());
            default -> Component.translatable(payload.messageKey());
        };

        SystemToast.add(
                minecraft.getToasts(),
                new SystemToast.SystemToastId(),
                Component.translatable("toast.guguaddons.chunk_claim.title"),
                message);
    }
}
