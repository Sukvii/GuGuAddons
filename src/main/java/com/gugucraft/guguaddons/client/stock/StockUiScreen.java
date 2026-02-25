package com.gugucraft.guguaddons.client.stock;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class StockUiScreen extends ModularUIScreen {
    private final Runnable onClosed;

    StockUiScreen(ModularUI modularUI, Component title, Runnable onClosed) {
        super(modularUI, title);
        this.onClosed = onClosed;
    }

    @Override
    public void onClose() {
        super.onClose();
        onClosed.run();
    }
}
