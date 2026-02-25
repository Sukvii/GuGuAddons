package com.gugucraft.guguaddons.stock.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.gugucraft.guguaddons.stock.StockCatalog;
import com.gugucraft.guguaddons.stock.StockDefinition;
import com.gugucraft.guguaddons.stock.StockMarketSavedData;

import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public final class StockUiService {
    public static final int PAGE_SIZE = 10;
    public static final int CHART_COLUMNS = 20;
    public static final int MINI_KLINE_CANDLES = 8;
    public static final int MINI_KLINE_HISTORY = 48;
    public static final int POPUP_KLINE_CANDLES = 24;
    public static final int[] LOT_OPTIONS = { 1, 5, 10, 25, 50, 100 };
    public static final int[] WINDOW_OPTIONS = { 36, 72, 144 };

    private StockUiService() {
    }

    public static StockUiSessionState defaultState() {
        return new StockUiSessionState(0, 0, 0, 0);
    }

    public static StockUiSessionState normalizeState(StockUiSessionState state) {
        int maxPage = getMaxPage();
        int selectedMax = Math.max(0, StockCatalog.size() - 1);
        return new StockUiSessionState(
                Mth.clamp(state.page(), 0, maxPage),
                Mth.clamp(state.selectedStock(), 0, selectedMax),
                Mth.clamp(state.lotIndex(), 0, LOT_OPTIONS.length - 1),
                Mth.clamp(state.windowIndex(), 0, WINDOW_OPTIONS.length - 1));
    }

    public static StockUiSessionState applyAction(
            ServerPlayer player,
            StockUiSessionState requestedState,
            StockUiAction action,
            int targetStock) {
        StockUiSessionState state = normalizeState(requestedState);
        StockMarketSavedData market = StockMarketSavedData.get(player.serverLevel().getServer());
        market.catchUp(player.serverLevel().getGameTime());

        switch (action) {
            case PREV_PAGE -> state = state.withPage(state.page() - 1);
            case NEXT_PAGE -> state = state.withPage(state.page() + 1);
            case SELECT_STOCK -> {
                if (targetStock >= 0 && targetStock < StockCatalog.size()) {
                    state = state.withSelectedStock(targetStock);
                }
            }
            case LOT_PREV -> {
                int nextIndex = state.lotIndex() - 1;
                if (nextIndex < 0) {
                    nextIndex = LOT_OPTIONS.length - 1;
                }
                state = state.withLotIndex(nextIndex);
                player.displayClientMessage(
                        Component.translatable("menu.guguaddons.stock.message.lot_size_changed", LOT_OPTIONS[nextIndex]),
                        true);
            }
            case LOT_NEXT -> {
                int nextIndex = (state.lotIndex() + 1) % LOT_OPTIONS.length;
                state = state.withLotIndex(nextIndex);
                player.displayClientMessage(
                        Component.translatable("menu.guguaddons.stock.message.lot_size_changed", LOT_OPTIONS[nextIndex]),
                        true);
            }
            case LOT_SET -> state = state.withLotIndex(Mth.clamp(targetStock, 0, LOT_OPTIONS.length - 1));
            case BUY -> buy(player, market, state.selectedStock(), LOT_OPTIONS[state.lotIndex()]);
            case SELL -> sell(player, market, state.selectedStock(), LOT_OPTIONS[state.lotIndex()]);
            case SELL_ALL -> sellAll(player, market, state.selectedStock());
            case WINDOW_NEXT -> state = state.withWindowIndex((state.windowIndex() + 1) % WINDOW_OPTIONS.length);
            case REFRESH -> player.displayClientMessage(
                    Component.translatable("menu.guguaddons.stock.message.refreshed"), true);
            case CLOSE -> {
            }
        }

        return normalizeState(state);
    }

    public static StockUiSnapshot createSnapshot(ServerPlayer player, StockUiSessionState requestedState) {
        StockUiSessionState state = normalizeState(requestedState);
        UUID playerId = player.getUUID();
        StockMarketSavedData market = StockMarketSavedData.get(player.serverLevel().getServer());
        market.catchUp(player.serverLevel().getGameTime());

        BankAccount account = Numismatics.BANK.getOrCreateAccount(playerId, BankAccount.Type.PLAYER);
        int balance = account.getBalance();
        int portfolioValue = market.getPortfolioValue(playerId);
        int totalHeldShares = market.getTotalHeldShares(playerId);

        int selectedStock = state.selectedStock();
        StockDefinition selectedDef = StockCatalog.get(selectedStock);
        int selectedPrice = market.getPrice(selectedStock);
        double selectedChange = market.getChangePercent(selectedStock);
        int selectedAsk = market.getAskPrice(selectedStock);
        int selectedBid = market.getBidPrice(selectedStock);
        int selectedHeldShares = market.getHolding(playerId, selectedStock);
        int selectedHoldingValue = (int) Math.min(Integer.MAX_VALUE, (long) selectedHeldShares * selectedPrice);

        int windowPoints = WINDOW_OPTIONS[state.windowIndex()];
        int[] selectedSeries = market.getRecentHistory(selectedStock, windowPoints);

        int selectedLow = Integer.MAX_VALUE;
        int selectedHigh = Integer.MIN_VALUE;
        long selectedSum = 0L;
        for (int value : selectedSeries) {
            selectedLow = Math.min(selectedLow, value);
            selectedHigh = Math.max(selectedHigh, value);
            selectedSum += value;
        }
        if (selectedSeries.length == 0) {
            selectedLow = selectedPrice;
            selectedHigh = selectedPrice;
        }
        int selectedAverage = (int) Math.round(selectedSum / (double) Math.max(1, selectedSeries.length));
        double selectedWindowChange = computeWindowChange(selectedSeries);
        double selectedVolatility = computeVolatility(selectedSeries);

        int[] chartPoints = bucketizeSeries(selectedSeries, CHART_COLUMNS);
        int chartMin = Integer.MAX_VALUE;
        int chartMax = Integer.MIN_VALUE;
        for (int point : chartPoints) {
            chartMin = Math.min(chartMin, point);
            chartMax = Math.max(chartMax, point);
        }
        if (chartPoints.length == 0) {
            chartPoints = new int[] { selectedPrice };
        }
        if (chartMin == Integer.MAX_VALUE || chartMax == Integer.MIN_VALUE) {
            chartMin = selectedPrice;
            chartMax = selectedPrice;
        }
        int[] selectedCandles = buildPopupCandles(selectedSeries, POPUP_KLINE_CANDLES);

        int page = state.page();
        int totalPages = getMaxPage() + 1;
        int startIndex = page * PAGE_SIZE;
        List<StockUiStockRow> rows = new ArrayList<>(PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) {
            int stockIndex = startIndex + i;
            if (stockIndex >= StockCatalog.size()) {
                break;
            }
            StockDefinition definition = StockCatalog.get(stockIndex);
            rows.add(new StockUiStockRow(
                    stockIndex,
                    definition.ticker(),
                    definition.company(),
                    market.getPrice(stockIndex),
                    market.getChangePercent(stockIndex),
                    market.getHolding(playerId, stockIndex),
                    stockIndex == selectedStock,
                    buildMiniCandles(market.getRecentHistory(stockIndex, MINI_KLINE_HISTORY), MINI_KLINE_CANDLES)));
        }

        return new StockUiSnapshot(
                page,
                totalPages,
                selectedStock,
                state.lotIndex(),
                LOT_OPTIONS[state.lotIndex()],
                state.windowIndex(),
                windowPoints,
                balance,
                portfolioValue,
                totalHeldShares,
                selectedDef.ticker(),
                selectedDef.company(),
                selectedPrice,
                selectedChange,
                selectedAsk,
                selectedBid,
                selectedHeldShares,
                selectedHoldingValue,
                selectedWindowChange,
                selectedLow,
                selectedHigh,
                selectedAverage,
                selectedVolatility,
                chartMin,
                chartMax,
                chartPoints,
                selectedCandles,
                List.copyOf(rows));
    }

    private static int getMaxPage() {
        return Math.max(0, (StockCatalog.size() - 1) / PAGE_SIZE);
    }

    private static void buy(ServerPlayer player, StockMarketSavedData market, int stockIndex, int shares) {
        UUID playerId = player.getUUID();
        BankAccount account = Numismatics.BANK.getOrCreateAccount(playerId, BankAccount.Type.PLAYER);
        int totalCost = market.quoteBuyTotal(stockIndex, shares);
        if (totalCost <= 0) {
            return;
        }

        if (!account.deduct(totalCost)) {
            player.displayClientMessage(
                    Component.translatable("menu.guguaddons.stock.error.insufficient_balance")
                            .withStyle(ChatFormatting.RED),
                    true);
            return;
        }
        if (!market.addHolding(playerId, stockIndex, shares)) {
            account.deposit(totalCost);
            player.displayClientMessage(
                    Component.translatable("menu.guguaddons.stock.error.position_limit").withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        StockDefinition definition = StockCatalog.get(stockIndex);
        player.displayClientMessage(
                Component.translatable("menu.guguaddons.stock.message.buy_success", shares, definition.ticker(),
                        formatSpurs(totalCost))
                        .withStyle(ChatFormatting.GREEN),
                true);
    }

    private static void sell(ServerPlayer player, StockMarketSavedData market, int stockIndex, int shares) {
        UUID playerId = player.getUUID();
        int owned = market.getHolding(playerId, stockIndex);
        int sellShares = Math.min(owned, Math.max(0, shares));
        if (sellShares <= 0) {
            player.displayClientMessage(
                    Component.translatable("menu.guguaddons.stock.error.no_shares").withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        int net = market.quoteSellNet(stockIndex, sellShares);
        if (!market.removeHolding(playerId, stockIndex, sellShares)) {
            player.displayClientMessage(
                    Component.translatable("menu.guguaddons.stock.error.sell_failed").withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        BankAccount account = Numismatics.BANK.getOrCreateAccount(playerId, BankAccount.Type.PLAYER);
        account.deposit(net);

        StockDefinition definition = StockCatalog.get(stockIndex);
        player.displayClientMessage(
                Component.translatable("menu.guguaddons.stock.message.sell_success", sellShares, definition.ticker(),
                        formatSpurs(net))
                        .withStyle(ChatFormatting.YELLOW),
                true);
    }

    private static void sellAll(ServerPlayer player, StockMarketSavedData market, int stockIndex) {
        UUID playerId = player.getUUID();
        int owned = market.removeAllHoldings(playerId, stockIndex);
        if (owned <= 0) {
            player.displayClientMessage(
                    Component.translatable("menu.guguaddons.stock.error.no_position").withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        int net = market.quoteSellNet(stockIndex, owned);
        BankAccount account = Numismatics.BANK.getOrCreateAccount(playerId, BankAccount.Type.PLAYER);
        account.deposit(net);

        StockDefinition definition = StockCatalog.get(stockIndex);
        player.displayClientMessage(
                Component.translatable("menu.guguaddons.stock.message.sell_all_success", definition.ticker(), owned,
                        formatSpurs(net))
                        .withStyle(ChatFormatting.GOLD),
                true);
    }

    private static int[] bucketizeSeries(int[] series, int columns) {
        if (series.length == 0) {
            return new int[] { 0 };
        }
        int bucketCount = Math.max(1, columns);
        int[] points = new int[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            int start = (int) Math.floor(i * series.length / (double) bucketCount);
            int endExclusive = (int) Math.floor((i + 1) * series.length / (double) bucketCount);
            if (endExclusive <= start) {
                endExclusive = Math.min(series.length, start + 1);
            }
            points[i] = series[Math.max(0, endExclusive - 1)];
        }
        return points;
    }

    private static int[] buildMiniCandles(int[] series, int candleCount) {
        int count = Math.max(1, candleCount);
        int[] candles = new int[count * 4];
        if (series.length == 0) {
            for (int i = 0; i < count; i++) {
                int offset = i * 4;
                candles[offset] = 0;
                candles[offset + 1] = 0;
                candles[offset + 2] = 0;
                candles[offset + 3] = 0;
            }
            return candles;
        }

        for (int i = 0; i < count; i++) {
            int start = (int) Math.floor(i * series.length / (double) count);
            int endExclusive = (int) Math.floor((i + 1) * series.length / (double) count);
            if (endExclusive <= start) {
                endExclusive = Math.min(series.length, start + 1);
            }
            start = Math.max(0, Math.min(start, series.length - 1));
            endExclusive = Math.max(start + 1, Math.min(endExclusive, series.length));

            int open = series[start];
            int close = series[endExclusive - 1];
            int high = Integer.MIN_VALUE;
            int low = Integer.MAX_VALUE;
            for (int j = start; j < endExclusive; j++) {
                high = Math.max(high, series[j]);
                low = Math.min(low, series[j]);
            }
            if (high == Integer.MIN_VALUE || low == Integer.MAX_VALUE) {
                high = close;
                low = close;
            }

            int offset = i * 4;
            candles[offset] = open;
            candles[offset + 1] = high;
            candles[offset + 2] = low;
            candles[offset + 3] = close;
        }
        return candles;
    }

    private static int[] buildPopupCandles(int[] series, int maxCandles) {
        int count = Math.max(1, maxCandles);
        int[] candles = new int[count * 4];
        if (series.length == 0) {
            return candles;
        }

        int minSamplesPerCandle = Math.min(3, series.length);
        for (int i = 0; i < count; i++) {
            int start = (int) Math.floor(i * series.length / (double) count);
            int endExclusive = (int) Math.floor((i + 1) * series.length / (double) count);
            if (endExclusive <= start) {
                endExclusive = Math.min(series.length, start + 1);
            }

            int sampleCount = Math.max(1, endExclusive - start);
            if (sampleCount < minSamplesPerCandle) {
                int center = Math.max(0, Math.min(series.length - 1, (start + endExclusive - 1) / 2));
                int left = center - (minSamplesPerCandle - 1) / 2;
                int right = left + minSamplesPerCandle;
                if (left < 0) {
                    right -= left;
                    left = 0;
                }
                if (right > series.length) {
                    left -= right - series.length;
                    right = series.length;
                }
                start = Math.max(0, left);
                endExclusive = Math.max(start + 1, right);
            }

            int open = series[start];
            int close = series[endExclusive - 1];
            int high = Integer.MIN_VALUE;
            int low = Integer.MAX_VALUE;
            for (int j = start; j < endExclusive; j++) {
                high = Math.max(high, series[j]);
                low = Math.min(low, series[j]);
            }
            if (high == Integer.MIN_VALUE || low == Integer.MAX_VALUE) {
                high = close;
                low = close;
            }

            int offset = i * 4;
            candles[offset] = open;
            candles[offset + 1] = high;
            candles[offset + 2] = low;
            candles[offset + 3] = close;
        }
        return candles;
    }

    private static double computeWindowChange(int[] series) {
        if (series.length <= 1) {
            return 0.0D;
        }
        int first = Math.max(1, series[0]);
        int last = series[series.length - 1];
        return (last - first) * 100.0D / first;
    }

    private static double computeVolatility(int[] series) {
        if (series.length <= 1) {
            return 0.0D;
        }
        double sum = 0.0D;
        int count = 0;
        for (int i = 1; i < series.length; i++) {
            int prev = Math.max(1, series[i - 1]);
            int now = series[i];
            double ret = (now - prev) * 100.0D / prev;
            sum += ret * ret;
            count++;
        }
        return Math.sqrt(sum / Math.max(1, count));
    }

    private static String formatSpurs(int amount) {
        return String.format(Locale.ROOT, "%,d sp", Math.max(0, amount));
    }
}
