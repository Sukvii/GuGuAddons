package com.gugucraft.guguaddons.stock.ui;

public record StockUiSessionState(
        int page,
        int selectedStock,
        int lotIndex,
        int windowIndex) {
    public StockUiSessionState withPage(int newPage) {
        return new StockUiSessionState(newPage, selectedStock, lotIndex, windowIndex);
    }

    public StockUiSessionState withSelectedStock(int newSelectedStock) {
        return new StockUiSessionState(page, newSelectedStock, lotIndex, windowIndex);
    }

    public StockUiSessionState withLotIndex(int newLotIndex) {
        return new StockUiSessionState(page, selectedStock, newLotIndex, windowIndex);
    }

    public StockUiSessionState withWindowIndex(int newWindowIndex) {
        return new StockUiSessionState(page, selectedStock, lotIndex, newWindowIndex);
    }
}
