package com.gugucraft.guguaddons.stock.ui;

import net.minecraft.nbt.CompoundTag;

public record StockUiStockRow(
        int stockIndex,
        String ticker,
        String company,
        int price,
        double changePercent,
        int heldShares,
        boolean selected,
        int[] miniCandles) {
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("StockIndex", stockIndex);
        tag.putString("Ticker", ticker);
        tag.putString("Company", company);
        tag.putInt("Price", price);
        tag.putDouble("ChangePercent", changePercent);
        tag.putInt("HeldShares", heldShares);
        tag.putBoolean("Selected", selected);
        tag.putIntArray("MiniCandles", miniCandles == null ? new int[0] : miniCandles);
        return tag;
    }

    public static StockUiStockRow fromTag(CompoundTag tag) {
        return new StockUiStockRow(
                tag.getInt("StockIndex"),
                tag.getString("Ticker"),
                tag.getString("Company"),
                tag.getInt("Price"),
                tag.getDouble("ChangePercent"),
                tag.getInt("HeldShares"),
                tag.getBoolean("Selected"),
                tag.getIntArray("MiniCandles"));
    }
}
