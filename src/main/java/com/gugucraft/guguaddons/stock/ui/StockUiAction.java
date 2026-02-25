package com.gugucraft.guguaddons.stock.ui;

public enum StockUiAction {
    REFRESH,
    PREV_PAGE,
    NEXT_PAGE,
    SELECT_STOCK,
    LOT_PREV,
    LOT_NEXT,
    LOT_SET,
    BUY,
    SELL,
    SELL_ALL,
    WINDOW_NEXT,
    CLOSE;

    private static final StockUiAction[] VALUES = values();

    public int id() {
        return ordinal();
    }

    public static StockUiAction fromId(int id) {
        if (id < 0 || id >= VALUES.length) {
            return REFRESH;
        }
        return VALUES[id];
    }
}
