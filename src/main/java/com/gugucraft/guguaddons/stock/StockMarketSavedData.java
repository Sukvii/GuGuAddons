package com.gugucraft.guguaddons.stock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

public class StockMarketSavedData extends SavedData {
    public static final String DATA_NAME = "guguaddons_stock_market";
    public static final int STOCK_COUNT = StockCatalog.size();
    public static final int TICKS_PER_STEP = 3600;
    public static final int HISTORY_LENGTH = 144;

    public static final int SPREAD_BPS = 35;
    public static final int BUY_FEE_BPS = 40;
    public static final int SELL_FEE_BPS = 45;

    private static final double STRUCTURAL_DRIFT_PENALTY = 0.00003D;
    private static final double UNDERVALUED_MEAN_REVERSION = 0.015D;
    private static final double OVERVALUED_MEAN_REVERSION = 0.026D;
    private static final double MOMENTUM_WEIGHT = 0.15D;
    private static final int IMPACT_BASE_BPS = 4;
    private static final double IMPACT_SQRT_SCALE_BPS = 2.0D;
    private static final int IMPACT_MAX_BPS = 180;

    private final int[] prices = new int[STOCK_COUNT];
    private final int[] previousPrices = new int[STOCK_COUNT];
    private final int[][] history = new int[STOCK_COUNT][HISTORY_LENGTH];
    private final int[] historyCursor = new int[STOCK_COUNT];
    private final int[] historySize = new int[STOCK_COUNT];
    private final double[] momenta = new double[STOCK_COUNT];
    private final Map<UUID, int[]> holdings = new HashMap<>();
    private final Map<UUID, Long> lastCarrySettlementStep = new HashMap<>();
    private final int[] stepTradeVolume = new int[STOCK_COUNT];

    private long lastUpdateTick = 0L;
    private long rngState = 0x9E3779B97F4A7C15L;
    private long tradeVolumeStep = -1L;
    private int regimeTicksRemaining = 0;
    private double regimeDrift = 0.0D;
    private double regimeVolatility = 1.0D;

    private StockMarketSavedData() {
        initializeFromCatalog();
    }

    public static SavedData.Factory<StockMarketSavedData> factory() {
        return new SavedData.Factory<>(StockMarketSavedData::new, StockMarketSavedData::load, null);
    }

    public static StockMarketSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    private static StockMarketSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        StockMarketSavedData data = new StockMarketSavedData();

        copyArray(tag.getIntArray("Prices"), data.prices);
        copyArray(tag.getIntArray("PreviousPrices"), data.previousPrices);
        copyArray(tag.getIntArray("HistoryCursor"), data.historyCursor);
        copyArray(tag.getIntArray("HistorySize"), data.historySize);

        ListTag historyList = tag.getList("History", Tag.TAG_COMPOUND);
        for (Tag value : historyList) {
            if (!(value instanceof CompoundTag stockTag)) {
                continue;
            }
            int stock = stockTag.getInt("Stock");
            if (stock < 0 || stock >= STOCK_COUNT) {
                continue;
            }
            int[] raw = stockTag.getIntArray("Values");
            copyArray(raw, data.history[stock]);
        }

        int[] momentumScaled = tag.getIntArray("MomentaScaled");
        for (int i = 0; i < Math.min(momentumScaled.length, STOCK_COUNT); i++) {
            data.momenta[i] = momentumScaled[i] / 1_000_000.0D;
        }

        data.lastUpdateTick = Math.max(0L, tag.getLong("LastUpdateTick"));
        data.rngState = tag.getLong("RngState");
        if (data.rngState == 0L) {
            data.rngState = 0x9E3779B97F4A7C15L;
        }
        data.regimeTicksRemaining = Math.max(0, tag.getInt("RegimeTicksRemaining"));
        data.regimeDrift = tag.getDouble("RegimeDrift");
        data.regimeVolatility = tag.getDouble("RegimeVolatility");
        if (data.regimeVolatility <= 0.0D) {
            data.regimeVolatility = 1.0D;
        }

        ListTag playerHoldings = tag.getList("Holdings", Tag.TAG_COMPOUND);
        for (Tag value : playerHoldings) {
            if (!(value instanceof CompoundTag playerTag) || !playerTag.hasUUID("Player")) {
                continue;
            }
            int[] raw = playerTag.getIntArray("Shares");
            int[] shares = new int[STOCK_COUNT];
            for (int i = 0; i < Math.min(raw.length, STOCK_COUNT); i++) {
                shares[i] = Math.max(0, raw[i]);
            }
            if (hasAnyShares(shares)) {
                data.holdings.put(playerTag.getUUID("Player"), shares);
            }
        }

        ListTag carrySettlement = tag.getList("CarrySettlement", Tag.TAG_COMPOUND);
        for (Tag value : carrySettlement) {
            if (!(value instanceof CompoundTag settlementTag) || !settlementTag.hasUUID("Player")) {
                continue;
            }
            long step = Math.max(0L, settlementTag.getLong("Step"));
            data.lastCarrySettlementStep.put(settlementTag.getUUID("Player"), step);
        }

        for (int i = 0; i < STOCK_COUNT; i++) {
            if (data.prices[i] <= 0) {
                data.prices[i] = StockCatalog.get(i).basePrice();
            }
            if (data.previousPrices[i] <= 0) {
                data.previousPrices[i] = data.prices[i];
            }
            if (data.historySize[i] <= 0) {
                data.history[i][0] = data.prices[i];
                data.historySize[i] = 1;
                data.historyCursor[i] = 1;
            } else {
                data.historySize[i] = Mth.clamp(data.historySize[i], 1, HISTORY_LENGTH);
                data.historyCursor[i] = Mth.clamp(data.historyCursor[i], 0, HISTORY_LENGTH - 1);
            }
        }

        data.refreshTradeVolumeWindow();

        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putIntArray("Prices", prices);
        tag.putIntArray("PreviousPrices", previousPrices);
        tag.putIntArray("HistoryCursor", historyCursor);
        tag.putIntArray("HistorySize", historySize);

        int[] momentumScaled = new int[STOCK_COUNT];
        for (int i = 0; i < STOCK_COUNT; i++) {
            momentumScaled[i] = (int) Mth.clamp(Math.round(momenta[i] * 1_000_000.0D), Integer.MIN_VALUE,
                    Integer.MAX_VALUE);
        }
        tag.putIntArray("MomentaScaled", momentumScaled);

        tag.putLong("LastUpdateTick", lastUpdateTick);
        tag.putLong("RngState", rngState);
        tag.putInt("RegimeTicksRemaining", regimeTicksRemaining);
        tag.putDouble("RegimeDrift", regimeDrift);
        tag.putDouble("RegimeVolatility", regimeVolatility);

        ListTag historyList = new ListTag();
        for (int i = 0; i < STOCK_COUNT; i++) {
            CompoundTag stockTag = new CompoundTag();
            stockTag.putInt("Stock", i);
            stockTag.putIntArray("Values", history[i]);
            historyList.add(stockTag);
        }
        tag.put("History", historyList);

        ListTag playerHoldings = new ListTag();
        for (Map.Entry<UUID, int[]> entry : holdings.entrySet()) {
            if (!hasAnyShares(entry.getValue())) {
                continue;
            }
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Player", entry.getKey());
            playerTag.putIntArray("Shares", entry.getValue());
            playerHoldings.add(playerTag);
        }
        tag.put("Holdings", playerHoldings);

        ListTag carrySettlement = new ListTag();
        for (Map.Entry<UUID, Long> entry : lastCarrySettlementStep.entrySet()) {
            CompoundTag settlementTag = new CompoundTag();
            settlementTag.putUUID("Player", entry.getKey());
            settlementTag.putLong("Step", Math.max(0L, entry.getValue()));
            carrySettlement.add(settlementTag);
        }
        tag.put("CarrySettlement", carrySettlement);
        return tag;
    }

    public void catchUp(long gameTime) {
        if (gameTime < 0L) {
            refreshTradeVolumeWindow();
            return;
        }
        if (lastUpdateTick == 0L) {
            lastUpdateTick = gameTime;
            refreshTradeVolumeWindow();
            return;
        }
        if (gameTime <= lastUpdateTick) {
            refreshTradeVolumeWindow();
            return;
        }

        long steps = (gameTime - lastUpdateTick) / TICKS_PER_STEP;
        if (steps <= 0L) {
            refreshTradeVolumeWindow();
            return;
        }

        steps = Math.min(steps, 24_000L);
        for (long i = 0L; i < steps; i++) {
            simulateStep();
        }
        lastUpdateTick += steps * TICKS_PER_STEP;
        refreshTradeVolumeWindow();
        setDirty();
    }

    public int getPrice(int stockIndex) {
        return prices[validateStockIndex(stockIndex)];
    }

    public int[] getRecentHistory(int stockIndex, int points) {
        int idx = validateStockIndex(stockIndex);
        int requested = Math.max(1, points);
        int size = Math.max(1, historySize[idx]);
        int actual = Math.min(requested, size);
        int[] out = new int[actual];

        int start = (historyCursor[idx] - actual + HISTORY_LENGTH) % HISTORY_LENGTH;
        for (int i = 0; i < actual; i++) {
            out[i] = history[idx][(start + i) % HISTORY_LENGTH];
        }
        return out;
    }

    public int getPreviousPrice(int stockIndex) {
        return previousPrices[validateStockIndex(stockIndex)];
    }

    public double getChangePercent(int stockIndex) {
        int current = getPrice(stockIndex);
        int prev = Math.max(1, getPreviousPrice(stockIndex));
        return ((current - prev) * 100.0D) / prev;
    }

    public int getAskPrice(int stockIndex) {
        return applyBps(getPrice(stockIndex), SPREAD_BPS, true);
    }

    public int getBidPrice(int stockIndex) {
        return applyBps(getPrice(stockIndex), -SPREAD_BPS, false);
    }

    public int quoteBuyTotal(int stockIndex, int shares) {
        int qty = Math.max(0, shares);
        if (qty == 0) {
            return 0;
        }
        int impact = computeImpactBps(stockIndex, qty);
        int executedAsk = applyBps(getPrice(stockIndex), SPREAD_BPS + impact, true);
        long gross = (long) executedAsk * qty;
        int fee = computeFee(gross, BUY_FEE_BPS);
        long total = gross + fee;
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public int quoteSellNet(int stockIndex, int shares) {
        int qty = Math.max(0, shares);
        if (qty == 0) {
            return 0;
        }
        int impact = computeImpactBps(stockIndex, qty);
        int executedBid = applyBps(getPrice(stockIndex), -(SPREAD_BPS + impact), false);
        long gross = (long) executedBid * qty;
        int fee = computeFee(gross, SELL_FEE_BPS);
        long net = Math.max(0L, gross - fee);
        return (int) Math.min(Integer.MAX_VALUE, net);
    }

    public void recordExecutedBuy(int stockIndex, int shares) {
        recordStepTradeVolume(stockIndex, shares);
    }

    public void recordExecutedSell(int stockIndex, int shares) {
        recordStepTradeVolume(stockIndex, shares);
    }

    public long consumeUnsettledCarrySteps(UUID playerId, long gameTime) {
        long currentStep = Math.max(0L, gameTime / TICKS_PER_STEP);
        Long previousStep = lastCarrySettlementStep.get(playerId);
        long settledFrom = previousStep == null ? currentStep : Math.min(previousStep, currentStep);
        long unsettled = Math.max(0L, currentStep - settledFrom);

        if (previousStep == null || previousStep != currentStep) {
            lastCarrySettlementStep.put(playerId, currentStep);
            setDirty();
        }

        return unsettled;
    }

    public int getHolding(UUID playerId, int stockIndex) {
        int[] portfolio = holdings.get(playerId);
        if (portfolio == null) {
            return 0;
        }
        return portfolio[validateStockIndex(stockIndex)];
    }

    public int getTotalHeldShares(UUID playerId) {
        int[] portfolio = holdings.get(playerId);
        if (portfolio == null) {
            return 0;
        }
        int total = 0;
        for (int shares : portfolio) {
            total += shares;
        }
        return total;
    }

    public int getPortfolioValue(UUID playerId) {
        int[] portfolio = holdings.get(playerId);
        if (portfolio == null) {
            return 0;
        }
        long total = 0L;
        for (int i = 0; i < STOCK_COUNT; i++) {
            total += (long) portfolio[i] * prices[i];
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public boolean addHolding(UUID playerId, int stockIndex, int shares) {
        int qty = Math.max(0, shares);
        if (qty == 0) {
            return false;
        }
        int idx = validateStockIndex(stockIndex);
        int[] portfolio = holdings.computeIfAbsent(playerId, ignored -> new int[STOCK_COUNT]);
        long result = (long) portfolio[idx] + qty;
        if (result > 1_000_000L) {
            return false;
        }
        portfolio[idx] = (int) result;
        lastCarrySettlementStep.putIfAbsent(playerId, Math.max(0L, lastUpdateTick / TICKS_PER_STEP));
        setDirty();
        return true;
    }

    public boolean removeHolding(UUID playerId, int stockIndex, int shares) {
        int qty = Math.max(0, shares);
        if (qty == 0) {
            return false;
        }
        int idx = validateStockIndex(stockIndex);
        int[] portfolio = holdings.get(playerId);
        if (portfolio == null || portfolio[idx] < qty) {
            return false;
        }
        portfolio[idx] -= qty;
        if (!hasAnyShares(portfolio)) {
            holdings.remove(playerId);
            lastCarrySettlementStep.remove(playerId);
        }
        setDirty();
        return true;
    }

    public int removeAllHoldings(UUID playerId, int stockIndex) {
        int idx = validateStockIndex(stockIndex);
        int[] portfolio = holdings.get(playerId);
        if (portfolio == null) {
            return 0;
        }
        int removed = portfolio[idx];
        if (removed <= 0) {
            return 0;
        }
        portfolio[idx] = 0;
        if (!hasAnyShares(portfolio)) {
            holdings.remove(playerId);
            lastCarrySettlementStep.remove(playerId);
        }
        setDirty();
        return removed;
    }

    private void initializeFromCatalog() {
        for (int i = 0; i < STOCK_COUNT; i++) {
            int initialPrice = StockCatalog.get(i).basePrice();
            prices[i] = initialPrice;
            previousPrices[i] = initialPrice;
            momenta[i] = 0.0D;
            Arrays.fill(history[i], initialPrice);
            historyCursor[i] = 1;
            historySize[i] = 1;
            stepTradeVolume[i] = 0;
        }
        holdings.clear();
        lastCarrySettlementStep.clear();
        tradeVolumeStep = -1L;
    }

    private void simulateStep() {
        if (regimeTicksRemaining <= 0) {
            rerollRegime();
        }
        regimeTicksRemaining--;

        double marketShock = nextNormal() * (0.0032D * regimeVolatility) + regimeDrift;

        for (int i = 0; i < STOCK_COUNT; i++) {
            StockDefinition def = StockCatalog.get(i);
            int current = Math.max(1, prices[i]);
            double idiosyncraticShock = nextNormal() * def.volatility();
            double anchor = Math.log(current / (double) Math.max(1, def.basePrice()));
            double meanReversionStrength = anchor >= 0.0D ? OVERVALUED_MEAN_REVERSION : UNDERVALUED_MEAN_REVERSION;
            double meanReversion = -meanReversionStrength * anchor;
            double structuralDrift = def.drift() - STRUCTURAL_DRIFT_PENALTY;

            double blendedShock = (marketShock * def.beta()) + idiosyncraticShock + structuralDrift;
            momenta[i] = momenta[i] * 0.60D + blendedShock * 0.40D;
            double totalReturn = blendedShock + meanReversion + (momenta[i] * MOMENTUM_WEIGHT);

            if (nextDouble() < 0.0025D) {
                totalReturn += (nextDouble() - 0.5D) * 0.10D;
            }

            totalReturn = Mth.clamp(totalReturn, -0.12D, 0.12D);

            int minPrice = Math.max(8, def.basePrice() / 5);
            int maxPrice = Math.max(minPrice + 1, def.basePrice() * 8);
            int nextPrice = (int) Math.round(current * (1.0D + totalReturn));
            nextPrice = Mth.clamp(nextPrice, minPrice, maxPrice);

            previousPrices[i] = current;
            prices[i] = nextPrice;
            pushHistory(i, nextPrice);
        }
    }

    private void rerollRegime() {
        regimeTicksRemaining = 180 + (int) (nextDouble() * 720.0D);
        regimeDrift = (nextDouble() - 0.55D) * 0.0010D;
        regimeVolatility = 0.75D + nextDouble() * 0.90D;
    }

    private long nextBits() {
        rngState = rngState * 6364136223846793005L + 1442695040888963407L;
        return rngState;
    }

    private double nextDouble() {
        return ((nextBits() >>> 11) * 0x1.0p-53);
    }

    private double nextNormal() {
        double u1 = Math.max(1.0E-12D, nextDouble());
        double u2 = nextDouble();
        return Math.sqrt(-2.0D * Math.log(u1)) * Math.cos(2.0D * Math.PI * u2);
    }

    private int computeImpactBps(int stockIndex, int shares) {
        int idx = validateStockIndex(stockIndex);
        int qty = Math.max(0, shares);
        if (qty <= 0) {
            return 0;
        }

        long effectiveQty = (long) qty + Math.max(0L, stepTradeVolume[idx]);
        double scaled = IMPACT_BASE_BPS + (Math.sqrt(effectiveQty) * IMPACT_SQRT_SCALE_BPS);
        return Mth.clamp((int) Math.round(scaled), IMPACT_BASE_BPS, IMPACT_MAX_BPS);
    }

    private void recordStepTradeVolume(int stockIndex, int shares) {
        int qty = Math.max(0, shares);
        if (qty <= 0) {
            return;
        }
        int idx = validateStockIndex(stockIndex);
        long nextVolume = (long) stepTradeVolume[idx] + qty;
        stepTradeVolume[idx] = (int) Math.min(Integer.MAX_VALUE, nextVolume);
    }

    private void refreshTradeVolumeWindow() {
        long currentStep = Math.max(0L, lastUpdateTick / TICKS_PER_STEP);
        if (tradeVolumeStep == currentStep) {
            return;
        }
        Arrays.fill(stepTradeVolume, 0);
        tradeVolumeStep = currentStep;
    }

    private static int applyBps(int baseValue, int bps, boolean roundUp) {
        long numerator = (long) baseValue * (10_000L + bps);
        long value = roundUp ? (long) Math.ceil(numerator / 10_000.0D) : (long) Math.floor(numerator / 10_000.0D);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, value));
    }

    private static int computeFee(long gross, int bps) {
        if (gross <= 0L) {
            return 0;
        }
        long fee = (gross * bps + 9_999L) / 10_000L;
        fee = Math.max(1L, fee);
        return (int) Math.min(Integer.MAX_VALUE, fee);
    }

    private static void copyArray(int[] from, int[] to) {
        Arrays.fill(to, 0);
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    private static boolean hasAnyShares(int[] shares) {
        for (int share : shares) {
            if (share > 0) {
                return true;
            }
        }
        return false;
    }

    private static int validateStockIndex(int stockIndex) {
        if (stockIndex < 0 || stockIndex >= STOCK_COUNT) {
            throw new IllegalArgumentException("Invalid stock index: " + stockIndex);
        }
        return stockIndex;
    }

    private void pushHistory(int stockIndex, int price) {
        int cursor = historyCursor[stockIndex];
        history[stockIndex][cursor] = price;
        historyCursor[stockIndex] = (cursor + 1) % HISTORY_LENGTH;
        historySize[stockIndex] = Math.min(HISTORY_LENGTH, historySize[stockIndex] + 1);
    }
}
