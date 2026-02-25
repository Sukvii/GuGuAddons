package com.gugucraft.guguaddons.stock.ui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public record StockUiSnapshot(
        int page,
        int totalPages,
        int selectedStock,
        int lotIndex,
        int lotSize,
        int windowIndex,
        int windowPoints,
        int balance,
        int portfolioValue,
        int totalHeldShares,
        String selectedTicker,
        String selectedCompany,
        int selectedPrice,
        double selectedChangePercent,
        int selectedAsk,
        int selectedBid,
        int selectedHeldShares,
        int selectedHoldingValue,
        double selectedWindowChangePercent,
        int selectedLow,
        int selectedHigh,
        int selectedAverage,
        double selectedVolatility,
        int chartMin,
        int chartMax,
        int[] chartPoints,
        int[] selectedCandles,
        List<StockUiStockRow> rows) {
    public static StockUiSnapshot empty() {
        return new StockUiSnapshot(
                0,
                1,
                0,
                0,
                1,
                0,
                36,
                0,
                0,
                0,
                "N/A",
                "N/A",
                0,
                0.0D,
                0,
                0,
                0,
                0,
                0.0D,
                0,
                0,
                0,
                0.0D,
                0,
                0,
                new int[] { 0 },
                new int[] { 0, 0, 0, 0 },
                List.of());
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Page", page);
        tag.putInt("TotalPages", totalPages);
        tag.putInt("SelectedStock", selectedStock);
        tag.putInt("LotIndex", lotIndex);
        tag.putInt("LotSize", lotSize);
        tag.putInt("WindowIndex", windowIndex);
        tag.putInt("WindowPoints", windowPoints);
        tag.putInt("Balance", balance);
        tag.putInt("PortfolioValue", portfolioValue);
        tag.putInt("TotalHeldShares", totalHeldShares);
        tag.putString("SelectedTicker", selectedTicker);
        tag.putString("SelectedCompany", selectedCompany);
        tag.putInt("SelectedPrice", selectedPrice);
        tag.putDouble("SelectedChangePercent", selectedChangePercent);
        tag.putInt("SelectedAsk", selectedAsk);
        tag.putInt("SelectedBid", selectedBid);
        tag.putInt("SelectedHeldShares", selectedHeldShares);
        tag.putInt("SelectedHoldingValue", selectedHoldingValue);
        tag.putDouble("SelectedWindowChangePercent", selectedWindowChangePercent);
        tag.putInt("SelectedLow", selectedLow);
        tag.putInt("SelectedHigh", selectedHigh);
        tag.putInt("SelectedAverage", selectedAverage);
        tag.putDouble("SelectedVolatility", selectedVolatility);
        tag.putInt("ChartMin", chartMin);
        tag.putInt("ChartMax", chartMax);
        tag.putIntArray("ChartPoints", chartPoints);
        tag.putIntArray("SelectedCandles", selectedCandles);

        ListTag rowTags = new ListTag();
        for (StockUiStockRow row : rows) {
            rowTags.add(row.toTag());
        }
        tag.put("Rows", rowTags);
        return tag;
    }

    public static StockUiSnapshot fromTag(CompoundTag tag) {
        int[] chartPoints = tag.getIntArray("ChartPoints");
        if (chartPoints.length == 0) {
            chartPoints = new int[] { 0 };
        }
        int[] selectedCandles = tag.getIntArray("SelectedCandles");
        if (selectedCandles.length == 0) {
            selectedCandles = new int[] { 0, 0, 0, 0 };
        }

        ListTag rowTags = tag.getList("Rows", Tag.TAG_COMPOUND);
        List<StockUiStockRow> rows = new ArrayList<>(rowTags.size());
        for (Tag raw : rowTags) {
            if (raw instanceof CompoundTag rowTag) {
                rows.add(StockUiStockRow.fromTag(rowTag));
            }
        }

        return new StockUiSnapshot(
                tag.getInt("Page"),
                Math.max(1, tag.getInt("TotalPages")),
                tag.getInt("SelectedStock"),
                tag.getInt("LotIndex"),
                Math.max(1, tag.getInt("LotSize")),
                tag.getInt("WindowIndex"),
                Math.max(1, tag.getInt("WindowPoints")),
                tag.getInt("Balance"),
                tag.getInt("PortfolioValue"),
                tag.getInt("TotalHeldShares"),
                tag.getString("SelectedTicker"),
                tag.getString("SelectedCompany"),
                tag.getInt("SelectedPrice"),
                tag.getDouble("SelectedChangePercent"),
                tag.getInt("SelectedAsk"),
                tag.getInt("SelectedBid"),
                tag.getInt("SelectedHeldShares"),
                tag.getInt("SelectedHoldingValue"),
                tag.getDouble("SelectedWindowChangePercent"),
                tag.getInt("SelectedLow"),
                tag.getInt("SelectedHigh"),
                tag.getInt("SelectedAverage"),
                tag.getDouble("SelectedVolatility"),
                tag.getInt("ChartMin"),
                tag.getInt("ChartMax"),
                chartPoints,
                selectedCandles,
                List.copyOf(rows));
    }
}
