package com.gugucraft.guguaddons.stock;

public record StockDefinition(
        String ticker,
        String company,
        int basePrice,
        double drift,
        double volatility,
        double beta) {
}
