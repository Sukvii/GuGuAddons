package com.gugucraft.guguaddons.stock;

import java.util.Locale;
import java.util.UUID;

import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class StockMarketMenu extends ChestMenu {
    private static final int ROWS = 6;
    private static final int SLOT_COUNT = 54;

    private static final int STOCK_FIRST_SLOT = 9;
    private static final int STOCK_SLOT_COUNT = 36;

    private static final int SLOT_PREV_PAGE = 0;
    private static final int SLOT_NEXT_PAGE = 8;
    private static final int SLOT_BALANCE = 45;
    private static final int SLOT_PORTFOLIO = 46;
    private static final int SLOT_LOT_SIZE = 47;
    private static final int SLOT_SELECTED = 48;
    private static final int SLOT_BUY = 49;
    private static final int SLOT_SELL = 50;
    private static final int SLOT_SELL_ALL = 51;
    private static final int SLOT_REFRESH = 52;
    private static final int SLOT_CLOSE = 53;

    private static final int[] LOT_OPTIONS = { 1, 5, 10, 25, 50, 100 };

    private final ServerPlayer player;
    private final StockMarketSavedData market;
    private final UUID playerId;
    private final StockViewContainer view;

    private int page = 0;
    private int selectedStock = 0;
    private int lotIndex = 0;

    private StockMarketMenu(int containerId, Inventory inventory, ServerPlayer player, StockMarketSavedData market,
            StockViewContainer view) {
        super(MenuType.GENERIC_9x6, containerId, inventory, view, ROWS);
        this.player = player;
        this.market = market;
        this.playerId = player.getUUID();
        this.view = view;
        refreshView(false);
    }

    public static StockMarketMenu create(int containerId, Inventory inventory, ServerPlayer player) {
        StockViewContainer container = new StockViewContainer();
        StockMarketSavedData market = StockMarketSavedData.get(player.serverLevel().getServer());
        return new StockMarketMenu(containerId, inventory, player, market, container);
    }

    public static SimpleMenuProvider provider(ServerPlayer player) {
        return new SimpleMenuProvider((containerId, inventory, ignored) -> create(containerId, inventory, player),
                Component.translatable("menu.guguaddons.stock.title"));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= SLOT_COUNT) {
            return;
        }

        if (slotId >= STOCK_FIRST_SLOT && slotId < STOCK_FIRST_SLOT + STOCK_SLOT_COUNT) {
            handleStockSlotClick(slotId, button, clickType);
            refreshView(false);
            return;
        }

        switch (slotId) {
            case SLOT_PREV_PAGE -> page = Math.max(0, page - 1);
            case SLOT_NEXT_PAGE -> page = Math.min(getMaxPage(), page + 1);
            case SLOT_LOT_SIZE -> cycleLotSize(button == 1 ? -1 : 1);
            case SLOT_BUY -> buy(selectedStock, LOT_OPTIONS[lotIndex]);
            case SLOT_SELL -> sell(selectedStock, LOT_OPTIONS[lotIndex]);
            case SLOT_SELL_ALL -> sellAll(selectedStock);
            case SLOT_REFRESH -> player.displayClientMessage(
                    Component.translatable("menu.guguaddons.stock.message.refreshed"), true);
            case SLOT_CLOSE -> {
                player.closeContainer();
                return;
            }
            default -> {
                return;
            }
        }
        refreshView(false);
    }

    private void handleStockSlotClick(int slotId, int button, ClickType clickType) {
        int stockIndex = page * STOCK_SLOT_COUNT + (slotId - STOCK_FIRST_SLOT);
        if (stockIndex < 0 || stockIndex >= StockCatalog.size()) {
            return;
        }
        selectedStock = stockIndex;

        if (clickType == ClickType.QUICK_MOVE) {
            sell(stockIndex, LOT_OPTIONS[lotIndex]);
            return;
        }
        if (clickType == ClickType.PICKUP && button == 1) {
            buy(stockIndex, LOT_OPTIONS[lotIndex]);
            return;
        }
        showSelectedHint(stockIndex);
    }

    private void cycleLotSize(int direction) {
        lotIndex += direction;
        if (lotIndex < 0) {
            lotIndex = LOT_OPTIONS.length - 1;
        } else if (lotIndex >= LOT_OPTIONS.length) {
            lotIndex = 0;
        }
        player.displayClientMessage(
                Component.translatable("menu.guguaddons.stock.message.lot_size_changed", LOT_OPTIONS[lotIndex]), true);
    }

    private void buy(int stockIndex, int shares) {
        market.catchUp(player.serverLevel().getGameTime());
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

        StockDefinition def = StockCatalog.get(stockIndex);
        player.displayClientMessage(
                Component.translatable("menu.guguaddons.stock.message.buy_success", shares, def.ticker(),
                        formatSpurs(totalCost))
                        .withStyle(ChatFormatting.GREEN),
                true);
    }

    private void sell(int stockIndex, int shares) {
        market.catchUp(player.serverLevel().getGameTime());
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

        StockDefinition def = StockCatalog.get(stockIndex);
        player.displayClientMessage(
                Component.translatable("menu.guguaddons.stock.message.sell_success", sellShares, def.ticker(),
                        formatSpurs(net))
                        .withStyle(ChatFormatting.YELLOW),
                true);
    }

    private void sellAll(int stockIndex) {
        market.catchUp(player.serverLevel().getGameTime());
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

        StockDefinition def = StockCatalog.get(stockIndex);
        player.displayClientMessage(
                Component.translatable("menu.guguaddons.stock.message.sell_all_success", def.ticker(), owned,
                        formatSpurs(net))
                        .withStyle(ChatFormatting.GOLD),
                true);
    }

    private void showSelectedHint(int stockIndex) {
        market.catchUp(player.serverLevel().getGameTime());
        StockDefinition def = StockCatalog.get(stockIndex);
        int price = market.getPrice(stockIndex);
        int ask = market.getAskPrice(stockIndex);
        int bid = market.getBidPrice(stockIndex);
        int owned = market.getHolding(playerId, stockIndex);
        double change = market.getChangePercent(stockIndex);

        player.displayClientMessage(Component.translatable("menu.guguaddons.stock.message.hint", def.ticker(),
                def.company(), formatSpurs(price), formatPercent(change), formatSpurs(ask), formatSpurs(bid), owned),
                true);
    }

    private void refreshView(boolean forceMessage) {
        market.catchUp(player.serverLevel().getGameTime());

        int maxPage = getMaxPage();
        page = Mth.clamp(page, 0, maxPage);
        selectedStock = Mth.clamp(selectedStock, 0, StockCatalog.size() - 1);
        int totalPages = maxPage + 1;

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            view.setItem(slot, fillerItem());
        }

        for (int i = 0; i < STOCK_SLOT_COUNT; i++) {
            int stockIndex = page * STOCK_SLOT_COUNT + i;
            int slot = STOCK_FIRST_SLOT + i;
            if (stockIndex >= StockCatalog.size()) {
                view.setItem(slot, emptyStockItem());
            } else {
                view.setItem(slot, stockItem(stockIndex));
            }
        }

        BankAccount account = Numismatics.BANK.getOrCreateAccount(playerId, BankAccount.Type.PLAYER);
        int balance = account.getBalance();
        int portfolioValue = market.getPortfolioValue(playerId);
        int totalHeldShares = market.getTotalHeldShares(playerId);

        view.setItem(SLOT_PREV_PAGE, named(Items.ARROW,
                Component.translatable("menu.guguaddons.stock.button.prev").withStyle(ChatFormatting.WHITE)));
        view.setItem(SLOT_NEXT_PAGE, named(Items.ARROW,
                Component.translatable("menu.guguaddons.stock.button.next").withStyle(ChatFormatting.WHITE)));
        view.setItem(SLOT_BALANCE, named(Items.GOLD_INGOT,
                Component.translatable("menu.guguaddons.stock.label.balance", formatSpurs(balance))
                        .withStyle(ChatFormatting.GOLD)));
        view.setItem(SLOT_PORTFOLIO, named(Items.CHEST,
                Component.translatable("menu.guguaddons.stock.label.portfolio", formatSpurs(portfolioValue),
                        totalHeldShares).withStyle(ChatFormatting.AQUA)));
        view.setItem(SLOT_LOT_SIZE, named(Items.HOPPER,
                Component.translatable("menu.guguaddons.stock.label.lot_size", LOT_OPTIONS[lotIndex])
                        .withStyle(ChatFormatting.YELLOW)));

        StockDefinition selected = StockCatalog.get(selectedStock);
        view.setItem(SLOT_SELECTED, named(Items.BOOK,
                Component.translatable("menu.guguaddons.stock.label.selected", selected.ticker(), selected.company())
                        .withStyle(ChatFormatting.GREEN)));
        view.setItem(SLOT_BUY, named(Items.LIME_DYE,
                Component.translatable("menu.guguaddons.stock.button.buy").withStyle(ChatFormatting.GREEN)));
        view.setItem(SLOT_SELL, named(Items.RED_DYE,
                Component.translatable("menu.guguaddons.stock.button.sell").withStyle(ChatFormatting.RED)));
        view.setItem(SLOT_SELL_ALL, named(Items.BLAZE_POWDER,
                Component.translatable("menu.guguaddons.stock.button.sell_all").withStyle(ChatFormatting.GOLD)));
        view.setItem(SLOT_REFRESH, named(Items.CLOCK,
                Component.translatable("menu.guguaddons.stock.button.refresh_page", page + 1, totalPages)
                        .withStyle(ChatFormatting.WHITE)));
        view.setItem(SLOT_CLOSE, named(Items.BARRIER,
                Component.translatable("menu.guguaddons.stock.button.close").withStyle(ChatFormatting.RED)));

        if (forceMessage) {
            player.displayClientMessage(Component.translatable("menu.guguaddons.stock.message.updated"), true);
        }

        broadcastChanges();
    }

    private ItemStack stockItem(int stockIndex) {
        StockDefinition def = StockCatalog.get(stockIndex);
        int price = market.getPrice(stockIndex);
        double change = market.getChangePercent(stockIndex);
        int held = market.getHolding(playerId, stockIndex);

        ChatFormatting color = colorForChange(change);
        ItemStack stack;
        if (stockIndex == selectedStock) {
            stack = new ItemStack(Items.COMPASS);
        } else if (change >= 3.0D) {
            stack = new ItemStack(Items.EMERALD_BLOCK);
        } else if (change >= 0.6D) {
            stack = new ItemStack(Items.LIME_WOOL);
        } else if (change <= -3.0D) {
            stack = new ItemStack(Items.REDSTONE_BLOCK);
        } else if (change <= -0.6D) {
            stack = new ItemStack(Items.RED_WOOL);
        } else {
            stack = new ItemStack(Items.YELLOW_WOOL);
        }

        stack.set(DataComponents.CUSTOM_NAME, Component
                .translatable("menu.guguaddons.stock.stock_entry", def.ticker(), formatSpurs(price), formatPercent(change),
                        held)
                .withStyle(color));
        return stack;
    }

    private static ItemStack emptyStockItem() {
        return named(Items.GRAY_STAINED_GLASS_PANE,
                Component.translatable("menu.guguaddons.stock.label.empty").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static ItemStack fillerItem() {
        return named(Items.BLACK_STAINED_GLASS_PANE, Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static ItemStack named(Item item, Component text) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, text);
        return stack;
    }

    private int getMaxPage() {
        return Math.max(0, (StockCatalog.size() - 1) / STOCK_SLOT_COUNT);
    }

    private static String formatSpurs(int amount) {
        return String.format(Locale.ROOT, "%,d sp", amount);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%+.2f%%", value);
    }

    private static ChatFormatting colorForChange(double value) {
        if (value > 0.01D) {
            return ChatFormatting.GREEN;
        }
        if (value < -0.01D) {
            return ChatFormatting.RED;
        }
        return ChatFormatting.YELLOW;
    }

    private static class StockViewContainer extends net.minecraft.world.SimpleContainer {
        private StockViewContainer() {
            super(SLOT_COUNT);
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return false;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            return ItemStack.EMPTY;
        }
    }
}
