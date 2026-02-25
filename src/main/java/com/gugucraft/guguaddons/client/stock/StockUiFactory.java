package com.gugucraft.guguaddons.client.stock;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.gugucraft.guguaddons.stock.ui.StockUiAction;
import com.gugucraft.guguaddons.stock.ui.StockUiService;
import com.gugucraft.guguaddons.stock.ui.StockUiSnapshot;
import com.gugucraft.guguaddons.stock.ui.StockUiStockRow;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.appliedenergistics.yoga.YogaPositionType;

@OnlyIn(Dist.CLIENT)
public final class StockUiFactory {
    private static final int POPUP_MARGIN = 8;
    private static final int POPUP_MIN_WIDTH = 170;
    private static final int POPUP_MIN_HEIGHT = 112;
    private static final int POPUP_CANDLE_COUNT = 24;

    private static final int RESIZE_LEFT = 1;
    private static final int RESIZE_RIGHT = 1 << 1;
    private static final int RESIZE_TOP = 1 << 2;
    private static final int RESIZE_BOTTOM = 1 << 3;
    private static final int POPUP_INTERACTION_NONE = 0;
    private static final int POPUP_INTERACTION_MOVE = -1;

    private StockUiFactory() {
    }

    public static StockUiView create(Player player, ActionDispatcher dispatcher) {
        Stylesheet style = Stylesheet.parse("""
                #oreui_root {
                  background: sdf(#151922, 1, 1, #252c3b);
                  padding-all: 8;
                  gap-all: 6;
                }

                #oreui_root text, #oreui_root label {
                  text-shadow: false;
                }

                #oreui_shell {
                  background: sdf(#2a303b, 1, 1, #616c7d);
                  padding-all: 6;
                  gap-all: 6;
                }

                #oreui_header {
                  background: sdf(#313947, 1, 1, #717b8b);
                  padding-all: 6;
                  gap-row: 2;
                }

                #oreui_header_title_row {
                  min-height: 14;
                  margin-bottom: 1;
                }

                #oreui_title {
                  text-color: #f2f5fb;
                  font-size: 11;
                  adaptive-height: true;
                }

                #oreui_balance {
                  text-color: #e8edf7;
                  adaptive-height: true;
                }

                #oreui_portfolio {
                  text-color: #c9d2e1;
                  adaptive-height: true;
                }

                #oreui_main_row {
                  flex-direction: row;
                  gap-column: 6;
                }

                .oreui-surface {
                  background: sdf(#303744, 1, 1, #667283);
                  padding-all: 4;
                  gap-all: 3;
                }

                #oreui_left {
                  width: 228;
                }

                #oreui_right {
                  flex: 1;
                }

                .oreui-row {
                  flex-direction: row;
                  gap-column: 4;
                }

                .oreui-row-fill button:host {
                  flex: 1;
                }

                #oreui_lot_block {
                  gap-all: 2;
                }

                #oreui_lot_header {
                  flex-direction: row;
                  align-items: center;
                  gap-column: 4;
                }

                #oreui_lot_label {
                  text-color: #d3dbe9;
                  width: 58;
                }

                #oreui_lot_value {
                  text-color: #f1f4fb;
                  horizontal-align: right;
                  flex: 1;
                }

                #oreui_lot_slider {
                  height: 12;
                  align-items: center;
                }

                #oreui_lot_slider .__scroller_scroll_container__ {
                  background: sdf(#222a36, 1, 1, #687486);
                  height: 4;
                  padding-all: 0;
                }

                #oreui_lot_slider .__scroller_scroll_bar__ {
                  base-background: sdf(#4ea93b, 1, 1, #2f6f24);
                  hover-background: sdf(#60c347, 1, 1, #3a8530);
                  pressed-background: sdf(#3a7f2d, 1, 1, #26541f) translate(0, 0.8);
                  width: 10;
                  height: 8;
                }

                #oreui_lot_slider .__scroller_head_button__,
                #oreui_lot_slider .__scroller_tail_button__ {
                  width: 0;
                  min-width: 0;
                  max-width: 0;
                  padding-all: 0;
                  margin-all: 0;
                }

                #oreui_window_row {
                  flex-direction: row;
                  align-items: center;
                  gap-column: 4;
                }

                #oreui_window_label {
                  text-color: #d3dbe9;
                  width: 36;
                }

                #oreui_window_button {
                  flex: 1;
                }

                #oreui_sell_all_row button:host {
                  width: stretch;
                }

                #oreui_page {
                  text-color: #e5ebf8;
                  horizontal-align: center;
                  width: 56;
                }

                #oreui_stock_list {
                  gap-row: 2;
                }

                .oreui-stock-row {
                  flex-direction: row;
                  gap-column: 3;
                  align-items: center;
                }

                .oreui-stock-row button:host {
                  flex: 1;
                  height: 17;
                }

                .oreui-stock-btn {
                  base-background: sdf(#363e4c, 1, 1, #808c9e);
                  hover-background: sdf(#434c5c, 1, 1, #9ca6b6);
                  pressed-background: sdf(#2f3642, 1, 1, #707b8e) translate(0, 0.9);
                }

                .oreui-stock-btn-selected {
                  base-background: sdf(#3f7f30, 1, 1, #25591d);
                  hover-background: sdf(#4f9b3d, 1, 1, #2e6c23);
                  pressed-background: sdf(#366d29, 1, 1, #224d1b) translate(0, 0.9);
                }

                .oreui-mini-kline {
                  width: 52;
                  height: 16;
                  background: sdf(#222a35, 1, 1, #616d7e);
                  padding-horizontal: 2;
                  padding-vertical: 1;
                  flex-direction: row;
                  gap-column: 1;
                  align-items: center;
                }

                .oreui-mini-candle {
                  width: 4;
                  height: 12;
                }

                .oreui-mini-wick {
                  background: sdf(#a8b2c2, 1);
                }

                .oreui-mini-body {
                  background: sdf(#a8b2c2, 1);
                }

                .oreui-candle-up {
                  background: sdf(#b75959, 1);
                }

                .oreui-candle-down {
                  background: sdf(#4ca63a, 1);
                }

                #oreui_selected_title {
                  text-color: #f1f4fb;
                  text-wrap: wrap;
                  adaptive-height: true;
                }

                .oreui-meta {
                  text-color: #d3dbe9;
                }

                #oreui_chart_frame {
                  background: sdf(#202632, 1, 1, #606b7c);
                  padding-all: 3;
                  position-type: relative;
                  min-height: 42;
                }

                #oreui_chart {
                  flex-direction: row;
                  align-items: flex-end;
                  gap-column: 1;
                  height: 36;
                }

                #oreui_chart_zoom_btn {
                  position-type: absolute;
                  width: 16;
                  height: 16;
                  min-width: 16;
                  min-height: 16;
                  padding-all: 0;
                  justify-content: center;
                  align-items: center;
                  base-background: sdf(#2f3745, 1, 1, #7e8a9f);
                  hover-background: sdf(#394456, 1, 1, #9cb1cf);
                  pressed-background: sdf(#27303d, 1, 1, #6b7a90) translate(0, 0.8);
                }

                #oreui_chart_zoom_icon {
                  width: 11;
                  height: 11;
                }

                .oreui-zoom-glyph {
                  background: sdf(#9fc8ff, 1);
                }

                .oreui-chart-bar {
                  width: 4;
                  min-height: 4;
                  background: sdf(#8591a3, 1);
                }

                .oreui-chart-up {
                  background: sdf(#b75959, 1);
                }

                .oreui-chart-down {
                  background: sdf(#4ca63a, 1);
                }

                .oreui-chart-flat {
                  background: sdf(#a4adba, 1);
                }

                #oreui_popup {
                  position-type: absolute;
                  z-index: 21;
                  background: sdf(#2c3442, 1, 1, #7b879a);
                  padding-all: 5;
                  gap-row: 4;
                }

                #oreui_popup_header {
                  flex-direction: row;
                  align-items: center;
                  gap-column: 3;
                  min-height: 14;
                  background: sdf(#27303d, 1, 1, #6d788a);
                  padding-horizontal: 2;
                  padding-vertical: 1;
                }

                #oreui_popup_title {
                  text-color: #eaf0fb;
                  flex: 1;
                }

                #oreui_popup_close {
                  width: 14;
                  min-width: 14;
                  height: 14;
                  min-height: 14;
                  padding-all: 0;
                  base-background: sdf(#3f4756, 1, 1, #8d98a9);
                  hover-background: sdf(#4d5868, 1, 1, #a8b3c4);
                  pressed-background: sdf(#38404d, 1, 1, #7c8799) translate(0, 0.8);
                }

                #oreui_popup_chart_frame {
                  background: sdf(#1e2530, 1, 1, #5f6c7f);
                  padding-all: 3;
                  flex: 1;
                }

                #oreui_popup_chart {
                  flex-direction: row;
                  align-items: stretch;
                  gap-column: 1;
                  height: stretch;
                  position-type: relative;
                }

                .oreui-popup-candle {
                  flex: 1;
                  min-width: 3;
                  height: stretch;
                  position-type: relative;
                }

                .oreui-popup-wick {
                  background: sdf(#b0bac9, 1);
                }

                .oreui-popup-body {
                  background: sdf(#b0bac9, 1);
                }

                .oreui-popup-trend-layer {
                  position-type: absolute;
                  left: 0;
                  right: 0;
                  top: 0;
                  bottom: 0;
                  z-index: 4;
                }

                .oreui-popup-handle {
                  position-type: absolute;
                  z-index: 24;
                }

                button:host {
                  base-background: sdf(#3b4351, 1, 1, #8f9aac);
                  hover-background: sdf(#4a5464, 1, 1, #aab4c4);
                  pressed-background: sdf(#313844, 1, 1, #7a8598) translate(0, 0.9);
                  padding-all: 3;
                  height: 17;
                }

                button:host .__button_text__ {
                  text-color: #eef2f8;
                }

                button.__oreui_btn_pressed__ .__button_text__ {
                  transform: translateY(0.9);
                }

                .oreui-btn-primary {
                  base-background: sdf(#4ea93b, 1, 1, #2f6f24);
                  hover-background: sdf(#60c347, 1, 1, #3a8530);
                  pressed-background: sdf(#428f32, 1, 1, #2b6122) translate(0, 0.9);
                }

                .oreui-btn-primary .__button_text__ {
                  text-color: #f4fff0;
                }
                """);

        Label balanceLabel = new Label();
        balanceLabel.setId("oreui_balance");
        Label portfolioLabel = new Label();
        portfolioLabel.setId("oreui_portfolio");
        Label pageLabel = new Label();
        pageLabel.setId("oreui_page");
        Label titleLabel = new Label();
        titleLabel.setId("oreui_title");
        titleLabel.setText(Component.translatable("menu.guguaddons.stock.title"));
        Label selectedTitle = new Label();
        selectedTitle.setId("oreui_selected_title");
        Label lotLabel = new Label();
        lotLabel.setId("oreui_lot_label");
        Label lotValueLabel = new Label();
        lotValueLabel.setId("oreui_lot_value");
        Scroller.Horizontal lotSlider = new Scroller.Horizontal();
        lotSlider.setId("oreui_lot_slider");
        lotSlider.setRange(0.0F, (float) (StockUiService.LOT_OPTIONS.length - 1));
        lotSlider.setScrollBarSize(9.0F);
        int lotStepCount = Math.max(1, StockUiService.LOT_OPTIONS.length - 1);
        lotSlider.setClampNormalizedValue(normalized -> {
            float clamped = Math.max(0.0F, Math.min(1.0F, normalized));
            return Math.round(clamped * lotStepCount) / (float) lotStepCount;
        });
        lotSlider.headButton(button -> {
            button.setDisplay(false);
            button.layout(layout -> {
                layout.width(0);
                layout.height(0);
            });
        });
        lotSlider.tailButton(button -> {
            button.setDisplay(false);
            button.layout(layout -> {
                layout.width(0);
                layout.height(0);
            });
        });
        lotSlider.scrollContainer(container -> container.layout(layout -> layout.height(4)));
        lotSlider.scrollBar(button -> {
            button.noText();
            button.layout(layout -> {
                layout.width(10);
                layout.height(8);
            });
            button.transform(transform -> transform.translate(0f, -2f));
        });
        AtomicBoolean lotSliderSyncing = new AtomicBoolean(false);
        AtomicInteger lastLotIndexSent = new AtomicInteger(-1);
        lotSlider.setOnValueChanged(value -> {
            if (lotSliderSyncing.get()) {
                return;
            }
            int lotIndex = Math.max(0, Math.min(StockUiService.LOT_OPTIONS.length - 1, Math.round(value)));
            lotSliderSyncing.set(true);
            lotSlider.setValue((float) lotIndex, false);
            lotSliderSyncing.set(false);
            if (lotIndex == lastLotIndexSent.get()) {
                return;
            }
            lastLotIndexSent.set(lotIndex);
            dispatcher.send(StockUiAction.LOT_SET, lotIndex);
        });

        Label windowLabel = new Label();
        windowLabel.setId("oreui_window_label");

        Label priceLabel = new Label();
        priceLabel.addClass("oreui-meta");
        Label dayChangeLabel = new Label();
        dayChangeLabel.addClass("oreui-meta");
        Label windowChangeLabel = new Label();
        windowChangeLabel.addClass("oreui-meta");
        Label quotesLabel = new Label();
        quotesLabel.addClass("oreui-meta");
        Label rangeLabel = new Label();
        rangeLabel.addClass("oreui-meta");
        Label averageLabel = new Label();
        averageLabel.addClass("oreui-meta");
        Label holdingLabel = new Label();
        holdingLabel.addClass("oreui-meta");
        Label valueLabel = new Label();
        valueLabel.addClass("oreui-meta");

        Button buyButton = new Button().setOnClick(event -> dispatcher.send(StockUiAction.BUY, -1));
        buyButton.addClass("oreui-btn-primary");
        withPressTextShift(buyButton);

        Button sellButton = new Button().setOnClick(event -> dispatcher.send(StockUiAction.SELL, -1));
        withPressTextShift(sellButton);

        Button sellAllButton = new Button().setOnClick(event -> dispatcher.send(StockUiAction.SELL_ALL, -1));
        withPressTextShift(sellAllButton);

        Button windowCycleButton = new Button().setOnClick(event -> dispatcher.send(StockUiAction.WINDOW_NEXT, -1));
        windowCycleButton.setId("oreui_window_button");
        withPressTextShift(windowCycleButton);

        Button refreshButton = new Button().setOnClick(event -> dispatcher.send(StockUiAction.REFRESH, -1));
        withPressTextShift(refreshButton);

        Button closeScreenButton = withPressTextShift(new Button()
                .setText(Component.translatable("menu.guguaddons.stock.button.close"))
                .setOnClick(event -> {
                    var modularUI = event.currentElement.getModularUI();
                    if (modularUI != null && modularUI.getScreen() != null) {
                        modularUI.getScreen().onClose();
                    }
                }));

        AtomicInteger[] rowTargets = new AtomicInteger[StockUiService.PAGE_SIZE];
        Button[] rowButtons = new Button[StockUiService.PAGE_SIZE];
        UIElement[] rowKlineContainers = new UIElement[StockUiService.PAGE_SIZE];
        UIElement[][] rowCandleWicks = new UIElement[StockUiService.PAGE_SIZE][StockUiService.MINI_KLINE_CANDLES];
        UIElement[][] rowCandleBodies = new UIElement[StockUiService.PAGE_SIZE][StockUiService.MINI_KLINE_CANDLES];
        UIElement stockList = new UIElement().setId("oreui_stock_list");
        for (int i = 0; i < StockUiService.PAGE_SIZE; i++) {
            int rowIndex = i;
            rowTargets[i] = new AtomicInteger(-1);
            Button rowButton = new Button()
                    .setText(Component.literal("-"))
                    .setOnClick(event -> {
                        int stockIndex = rowTargets[rowIndex].get();
                        if (stockIndex >= 0) {
                            dispatcher.send(StockUiAction.SELECT_STOCK, stockIndex);
                        }
                    });
            rowButton.addClass("oreui-stock-btn");
            rowButtons[i] = withPressTextShift(rowButton);

            UIElement miniKline = new UIElement().addClass("oreui-mini-kline");
            rowKlineContainers[i] = miniKline;
            for (int candle = 0; candle < StockUiService.MINI_KLINE_CANDLES; candle++) {
                UIElement candleCol = new UIElement().addClass("oreui-mini-candle");
                UIElement wick = new UIElement().addClass("oreui-mini-wick");
                UIElement body = new UIElement().addClass("oreui-mini-body");
                wick.layout(layout -> {
                    layout.positionType(YogaPositionType.ABSOLUTE);
                    layout.left(1);
                    layout.bottom(0);
                    layout.width(1);
                    layout.height(2);
                });
                body.layout(layout -> {
                    layout.positionType(YogaPositionType.ABSOLUTE);
                    layout.left(0);
                    layout.bottom(0);
                    layout.width(3);
                    layout.height(2);
                });
                candleCol.addChildren(wick, body);
                miniKline.addChild(candleCol);
                rowCandleWicks[i][candle] = wick;
                rowCandleBodies[i][candle] = body;
            }

            UIElement row = new UIElement().addClass("oreui-stock-row").addChildren(rowButtons[i], miniKline);
            stockList.addChild(row);
        }

        UIElement[] chartBars = new UIElement[StockUiService.CHART_COLUMNS];
        UIElement chart = new UIElement().setId("oreui_chart");
        for (int i = 0; i < chartBars.length; i++) {
            chartBars[i] = new UIElement()
                    .addClasses("oreui-chart-bar", "oreui-chart-flat")
                    .layout(layout -> layout.height(6));
            chart.addChild(chartBars[i]);
        }

        AtomicInteger popupX = new AtomicInteger(258);
        AtomicInteger popupY = new AtomicInteger(90);
        AtomicInteger popupWidth = new AtomicInteger(184);
        AtomicInteger popupHeight = new AtomicInteger(124);
        AtomicInteger popupInteractionMode = new AtomicInteger(POPUP_INTERACTION_NONE);
        AtomicBoolean popupPointerReady = new AtomicBoolean(false);
        AtomicInteger popupPointerX = new AtomicInteger(0);
        AtomicInteger popupPointerY = new AtomicInteger(0);
        AtomicReference<int[]> popupSourceCandles = new AtomicReference<>(new int[] { 0, 0, 0, 0 });

        Label popupTitleLabel = new Label();
        popupTitleLabel.setId("oreui_popup_title");

        UIElement[] popupCandleWicks = new UIElement[POPUP_CANDLE_COUNT];
        UIElement[] popupCandleBodies = new UIElement[POPUP_CANDLE_COUNT];
        UIElement popupChart = new UIElement().setId("oreui_popup_chart")
                .setOverflowVisible(false)
                .layout(layout -> layout.heightStretch());
        for (int i = 0; i < POPUP_CANDLE_COUNT; i++) {
            UIElement candle = new UIElement().addClass("oreui-popup-candle")
                    .layout(layout -> layout.heightStretch());
            UIElement wick = new UIElement().addClass("oreui-popup-wick");
            UIElement body = new UIElement().addClass("oreui-popup-body");
            wick.layout(layout -> {
                layout.positionType(YogaPositionType.ABSOLUTE);
                layout.left(1);
                layout.bottom(0);
                layout.width(1);
                layout.height(2);
            });
            body.layout(layout -> {
                layout.positionType(YogaPositionType.ABSOLUTE);
                layout.left(1);
                layout.bottom(0);
                layout.width(2);
                layout.height(2);
            });
            candle.addChildren(wick, body);
            popupChart.addChild(candle);
            popupCandleWicks[i] = wick;
            popupCandleBodies[i] = body;
        }
        UIElement popupTrendLayer = new PopupTrendLineOverlay(popupSourceCandles)
                .addClass("oreui-popup-trend-layer")
                .setAllowHitTest(false);
        popupTrendLayer.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.left(0);
            layout.top(0);
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        popupChart.addChild(popupTrendLayer);

        UIElement popupWindow = new UIElement()
                .setId("oreui_popup")
                .setVisible(false)
                .layout(layout -> {
                    layout.positionType(YogaPositionType.ABSOLUTE);
                    layout.left(popupX.get());
                    layout.top(popupY.get());
                    layout.width(popupWidth.get());
                    layout.height(popupHeight.get());
                });

        Button popupCloseButton = withPressTextShift(new Button()
                .setText(Component.literal("x"))
                .setOnClick(event -> {
                    popupInteractionMode.set(POPUP_INTERACTION_NONE);
                    popupPointerReady.set(false);
                    popupWindow.setVisible(false);
                }));
        popupCloseButton.setId("oreui_popup_close");
        popupCloseButton.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(POPUP_INTERACTION_NONE);
                popupPointerReady.set(false);
                event.stopPropagation();
            }
        });

        UIElement popupHeader = new UIElement()
                .setId("oreui_popup_header")
                .addChildren(popupTitleLabel, popupCloseButton);
        popupHeader.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(POPUP_INTERACTION_MOVE);
                popupPointerReady.set(false);
                event.stopImmediatePropagation();
            }
        });

        UIElement popupTopHandle = new UIElement().addClass("oreui-popup-handle");
        popupTopHandle.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.left(5);
            layout.right(5);
            layout.top(0);
            layout.height(4);
        });
        popupTopHandle.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(RESIZE_TOP);
                popupPointerReady.set(false);
                event.stopImmediatePropagation();
            }
        });

        UIElement popupBottomHandle = new UIElement().addClass("oreui-popup-handle");
        popupBottomHandle.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.left(5);
            layout.right(5);
            layout.bottom(0);
            layout.height(4);
        });
        popupBottomHandle.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(RESIZE_BOTTOM);
                popupPointerReady.set(false);
                event.stopImmediatePropagation();
            }
        });

        UIElement popupLeftHandle = new UIElement().addClass("oreui-popup-handle");
        popupLeftHandle.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.left(0);
            layout.top(5);
            layout.bottom(5);
            layout.width(4);
        });
        popupLeftHandle.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(RESIZE_LEFT);
                popupPointerReady.set(false);
                event.stopImmediatePropagation();
            }
        });

        UIElement popupRightHandle = new UIElement().addClass("oreui-popup-handle");
        popupRightHandle.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.right(0);
            layout.top(5);
            layout.bottom(5);
            layout.width(4);
        });
        popupRightHandle.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(RESIZE_RIGHT);
                popupPointerReady.set(false);
                event.stopImmediatePropagation();
            }
        });

        UIElement popupTopLeftHandle = new UIElement().addClass("oreui-popup-handle");
        popupTopLeftHandle.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.left(0);
            layout.top(0);
            layout.width(6);
            layout.height(6);
        });
        popupTopLeftHandle.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(RESIZE_LEFT | RESIZE_TOP);
                popupPointerReady.set(false);
                event.stopImmediatePropagation();
            }
        });

        UIElement popupTopRightHandle = new UIElement().addClass("oreui-popup-handle");
        popupTopRightHandle.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.right(0);
            layout.top(0);
            layout.width(6);
            layout.height(6);
        });
        popupTopRightHandle.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(RESIZE_RIGHT | RESIZE_TOP);
                popupPointerReady.set(false);
                event.stopImmediatePropagation();
            }
        });

        UIElement popupBottomLeftHandle = new UIElement().addClass("oreui-popup-handle");
        popupBottomLeftHandle.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.left(0);
            layout.bottom(0);
            layout.width(6);
            layout.height(6);
        });
        popupBottomLeftHandle.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(RESIZE_LEFT | RESIZE_BOTTOM);
                popupPointerReady.set(false);
                event.stopImmediatePropagation();
            }
        });

        UIElement popupBottomRightHandle = new UIElement().addClass("oreui-popup-handle");
        popupBottomRightHandle.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.right(0);
            layout.bottom(0);
            layout.width(6);
            layout.height(6);
        });
        popupBottomRightHandle.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(RESIZE_RIGHT | RESIZE_BOTTOM);
                popupPointerReady.set(false);
                event.stopImmediatePropagation();
            }
        });

        UIElement popupChartFrame = new UIElement()
                .setId("oreui_popup_chart_frame")
                .layout(layout -> layout.flex(1))
                .addChild(popupChart);

        popupWindow.addChildren(
                popupHeader,
                popupChartFrame,
                popupTopHandle,
                popupBottomHandle,
                popupLeftHandle,
                popupRightHandle,
                popupTopLeftHandle,
                popupTopRightHandle,
                popupBottomLeftHandle,
                popupBottomRightHandle);

        Button zoomChartButton = withPressTextShift(new Button().noText());
        zoomChartButton.setId("oreui_chart_zoom_btn");
        zoomChartButton.addChild(createZoomGlyphIcon());
        zoomChartButton.layout(layout -> {
            layout.positionType(YogaPositionType.ABSOLUTE);
            layout.right(1);
            layout.bottom(1);
            layout.width(16);
            layout.height(16);
        });

        UIElement mainChartFrame = new UIElement()
                .setId("oreui_chart_frame")
                .addChildren(chart, zoomChartButton);

        UIElement root = new UIElement()
                .setId("oreui_root")
                .layout(layout -> layout.width(460))
                .addChildren(
                        new UIElement()
                                .setId("oreui_shell")
                                .addChildren(
                                        new UIElement()
                                                .setId("oreui_header")
                                                .addChildren(
                                                        new UIElement()
                                                                .setId("oreui_header_title_row")
                                                                .addChild(titleLabel),
                                                        balanceLabel,
                                                        portfolioLabel),
                                        new UIElement()
                                                .setId("oreui_main_row")
                                                .addChildren(
                                                        new UIElement()
                                                                .setId("oreui_left")
                                                                .addClass("oreui-surface")
                                                                .addChildren(
                                                                        new UIElement()
                                                                                .addClasses("oreui-row", "oreui-row-fill")
                                                                                .addChildren(
                                                                                        withPressTextShift(new Button()
                                                                                                .setText(Component.translatable(
                                                                                                        "menu.guguaddons.stock.button.prev"))
                                                                                                .setOnClick(event -> dispatcher.send(
                                                                                                        StockUiAction.PREV_PAGE, -1))),
                                                                                        pageLabel,
                                                                                        withPressTextShift(new Button()
                                                                                                .setText(Component.translatable(
                                                                                                        "menu.guguaddons.stock.button.next"))
                                                                                                .setOnClick(event -> dispatcher.send(
                                                                                                        StockUiAction.NEXT_PAGE, -1)))),
                                                                        stockList),
                                                        new UIElement()
                                                                .setId("oreui_right")
                                                                .addClass("oreui-surface")
                                                                .addChildren(
                                                                        selectedTitle,
                                                                        priceLabel,
                                                                        dayChangeLabel,
                                                                        windowChangeLabel,
                                                                        quotesLabel,
                                                                        rangeLabel,
                                                                        averageLabel,
                                                                        holdingLabel,
                                                                        valueLabel,
                                                                        mainChartFrame,
                                                                        new UIElement()
                                                                                .setId("oreui_lot_block")
                                                                                .addChildren(
                                                                                        new UIElement()
                                                                                                .setId("oreui_lot_header")
                                                                                                .addChildren(
                                                                                                        lotLabel,
                                                                                                        lotValueLabel),
                                                                                        lotSlider),
                                                                        new UIElement()
                                                                                .setId("oreui_window_row")
                                                                                .addChildren(
                                                                                        windowLabel,
                                                                                        windowCycleButton),
                                                                        new UIElement()
                                                                                .addClasses("oreui-row", "oreui-row-fill")
                                                                                .addChildren(
                                                                                        buyButton,
                                                                                        sellButton),
                                                                        new UIElement()
                                                                                .setId("oreui_sell_all_row")
                                                                                .addClass("oreui-row")
                                                                                .addChildren(
                                                                                        sellAllButton),
                                                                        new UIElement()
                                                                                .addClasses("oreui-row", "oreui-row-fill")
                                                                                .addChildren(
                                                                                        refreshButton,
                                                                                        closeScreenButton)))),
                        popupWindow);

        Runnable applyPopupLayout = () -> {
            int[] rect = clampPopupRect(
                    popupX.get(),
                    popupY.get(),
                    popupWidth.get(),
                    popupHeight.get(),
                    root);
            popupX.set(rect[0]);
            popupY.set(rect[1]);
            popupWidth.set(rect[2]);
            popupHeight.set(rect[3]);
            applyPopupRect(popupWindow, rect[0], rect[1], rect[2], rect[3]);
        };

        Runnable renderPopupKline = () -> {
            int contentHeight = Math.round(popupChart.getContentHeight()) - 1;
            int chartHeight = Math.max(30, contentHeight > 0 ? contentHeight : popupHeight.get() - 38);
            int[] popupCandles = popupSourceCandles.get();
            renderCandles(popupCandleWicks, popupCandleBodies, popupCandles, chartHeight, "oreui-popup-wick",
                    "oreui-popup-body");
        };

        root.addEventListener(UIEvents.MOUSE_MOVE, event -> {
            if (!popupWindow.isVisible()) {
                return;
            }
            int interactionMode = popupInteractionMode.get();
            if (interactionMode == POPUP_INTERACTION_NONE) {
                return;
            }
            if (!root.isMouseDown(0)) {
                popupInteractionMode.set(POPUP_INTERACTION_NONE);
                popupPointerReady.set(false);
                return;
            }

            int mouseX = Math.round(event.x);
            int mouseY = Math.round(event.y);
            if (!popupPointerReady.get()) {
                popupPointerX.set(mouseX);
                popupPointerY.set(mouseY);
                popupPointerReady.set(true);
                return;
            }

            int deltaX = mouseX - popupPointerX.get();
            int deltaY = mouseY - popupPointerY.get();
            if (deltaX == 0 && deltaY == 0) {
                return;
            }
            popupPointerX.set(mouseX);
            popupPointerY.set(mouseY);

            int x = popupX.get();
            int y = popupY.get();
            int width = popupWidth.get();
            int height = popupHeight.get();

            if (interactionMode == POPUP_INTERACTION_MOVE) {
                x += deltaX;
                y += deltaY;
            } else {
                if ((interactionMode & RESIZE_LEFT) != 0) {
                    x += deltaX;
                    width -= deltaX;
                }
                if ((interactionMode & RESIZE_RIGHT) != 0) {
                    width += deltaX;
                }
                if ((interactionMode & RESIZE_TOP) != 0) {
                    y += deltaY;
                    height -= deltaY;
                }
                if ((interactionMode & RESIZE_BOTTOM) != 0) {
                    height += deltaY;
                }
            }

            int[] rect = clampPopupRect(x, y, width, height, root);
            popupX.set(rect[0]);
            popupY.set(rect[1]);
            popupWidth.set(rect[2]);
            popupHeight.set(rect[3]);
            applyPopupRect(popupWindow, rect[0], rect[1], rect[2], rect[3]);
            renderPopupKline.run();
        }, true);

        root.addEventListener(UIEvents.MOUSE_UP, event -> {
            if (event.button == 0) {
                popupInteractionMode.set(POPUP_INTERACTION_NONE);
                popupPointerReady.set(false);
            }
        }, true);
        root.addEventListener(UIEvents.MOUSE_LEAVE, event -> {
            popupInteractionMode.set(POPUP_INTERACTION_NONE);
            popupPointerReady.set(false);
        }, true);
        root.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            if (!popupWindow.isVisible()) {
                return;
            }
            applyPopupLayout.run();
            renderPopupKline.run();
        }, true);

        zoomChartButton.setOnClick(event -> {
            popupWindow.setVisible(true);
            popupInteractionMode.set(POPUP_INTERACTION_NONE);
            popupPointerReady.set(false);
            applyPopupLayout.run();
            renderPopupKline.run();
        });

        Consumer<StockUiSnapshot> applySnapshot = snapshot -> {
            balanceLabel.setText(Component.translatable(
                    "menu.guguaddons.stock.label.balance",
                    formatSpurs(snapshot.balance())));
            portfolioLabel.setText(Component.translatable(
                    "menu.guguaddons.stock.label.portfolio",
                    formatSpurs(snapshot.portfolioValue()),
                    snapshot.totalHeldShares()));
            pageLabel.setText(Component.literal(String.format(
                    Locale.ROOT,
                    "%d / %d",
                    snapshot.page() + 1,
                    snapshot.totalPages())));

            selectedTitle.setText(Component.translatable(
                    "menu.guguaddons.stock.label.selected",
                    snapshot.selectedTicker(),
                    snapshot.selectedCompany()));
            popupTitleLabel.setText(Component.literal(snapshot.selectedTicker() + " K-Line"));
            priceLabel.setText(Component.translatable(
                    "menu.guguaddons.stock.detail.label.price",
                    formatSpurs(snapshot.selectedPrice())).withStyle(ChatFormatting.GOLD));
            dayChangeLabel.setText(Component.literal("Day: " + formatPercent(snapshot.selectedChangePercent()))
                    .withStyle(colorForChange(snapshot.selectedChangePercent())));
            windowChangeLabel.setText(Component.translatable(
                    "menu.guguaddons.stock.detail.label.change",
                    formatPercent(snapshot.selectedWindowChangePercent()))
                    .withStyle(colorForChange(snapshot.selectedWindowChangePercent())));
            quotesLabel.setText(Component.translatable(
                    "menu.guguaddons.stock.detail.label.quotes",
                    formatSpurs(snapshot.selectedAsk()),
                    formatSpurs(snapshot.selectedBid())));
            rangeLabel.setText(Component.translatable(
                    "menu.guguaddons.stock.detail.label.range",
                    formatSpurs(snapshot.selectedLow()),
                    formatSpurs(snapshot.selectedHigh())));
            averageLabel.setText(Component.translatable(
                    "menu.guguaddons.stock.detail.label.average",
                    formatSpurs(snapshot.selectedAverage()),
                    formatPercent(snapshot.selectedVolatility())));
            holdingLabel.setText(Component.translatable(
                    "menu.guguaddons.stock.detail.label.holding",
                    snapshot.selectedHeldShares()));
            valueLabel.setText(Component.translatable(
                    "menu.guguaddons.stock.detail.label.value",
                    formatSpurs(snapshot.selectedHoldingValue())).withStyle(ChatFormatting.GREEN));
            lotLabel.setText(Component.literal("\u4ea4\u6613\u624b\u6570"));
            lotValueLabel.setText(Component.literal(String.format(Locale.ROOT, "%d", snapshot.lotSize())));
            lotSliderSyncing.set(true);
            lotSlider.setValue((float) snapshot.lotIndex(), false);
            lotSliderSyncing.set(false);
            lastLotIndexSent.set(snapshot.lotIndex());
            windowLabel.setText(Component.literal("\u7a97\u53e3"));
            windowCycleButton.setText(Component.translatable(
                    "menu.guguaddons.stock.detail.button.window",
                    snapshot.windowPoints()));
            refreshButton.setText(Component.translatable("menu.guguaddons.stock.detail.button.refresh"));

            buyButton.setText(Component.literal("\u4e70\u5165 x" + snapshot.lotSize()));
            sellButton.setText(Component.literal("\u5356\u51fa x" + snapshot.lotSize()));
            sellAllButton.setText(Component.literal("\u5168\u90e8\u5356\u51fa"));

            for (int i = 0; i < rowButtons.length; i++) {
                Button button = rowButtons[i];
                if (i < snapshot.rows().size()) {
                    StockUiStockRow row = snapshot.rows().get(i);
                    rowTargets[i].set(row.stockIndex());
                    button.setVisible(true);
                    button.setActive(true);
                    button.setText(Component.translatable(
                            "menu.guguaddons.stock.stock_entry",
                            row.ticker(),
                            formatSpurs(row.price()),
                            formatPercent(row.changePercent()),
                            row.heldShares()).withStyle(colorForChange(row.changePercent())));
                    button.setClasses("oreui-stock-btn");
                    if (row.selected()) {
                        button.addClass("oreui-stock-btn-selected");
                    }
                    rowKlineContainers[i].setVisible(true);
                    renderMiniKline(rowCandleWicks[i], rowCandleBodies[i], row.miniCandles());
                } else {
                    rowTargets[i].set(-1);
                    button.setActive(false);
                    button.setVisible(false);
                    rowKlineContainers[i].setVisible(false);
                }
            }

            int[] points = snapshot.chartPoints();
            int min = snapshot.chartMin();
            int max = snapshot.chartMax();
            int range = Math.max(1, max - min);
            for (int i = 0; i < chartBars.length; i++) {
                UIElement bar = chartBars[i];
                if (i >= points.length) {
                    bar.setVisible(false);
                    continue;
                }

                int value = points[i];
                int height = 5 + Math.round((value - min) * 28.0F / range);
                bar.layout(layout -> layout.height(height));
                bar.setVisible(true);
                bar.setClasses("oreui-chart-bar");
                if (i == 0) {
                    bar.addClass("oreui-chart-flat");
                } else if (value > points[i - 1]) {
                    bar.addClass("oreui-chart-up");
                } else if (value < points[i - 1]) {
                    bar.addClass("oreui-chart-down");
                } else {
                    bar.addClass("oreui-chart-flat");
                }
            }

            popupSourceCandles.set(snapshot.selectedCandles().clone());
            renderPopupKline.run();
        };

        applySnapshot.accept(StockUiSnapshot.empty());
        return new StockUiView(ModularUI.of(UI.of(root, style), player), applySnapshot);
    }

    private static UIElement createZoomGlyphIcon() {
        UIElement icon = new UIElement().setId("oreui_chart_zoom_icon");
        icon.addChildren(
                createGlyphBlock(3, 2, 4, 1),
                createGlyphBlock(2, 3, 1, 3),
                createGlyphBlock(7, 3, 1, 3),
                createGlyphBlock(3, 6, 4, 1),
                createGlyphBlock(7, 7, 1, 1),
                createGlyphBlock(8, 8, 1, 1),
                createGlyphBlock(9, 9, 1, 1));
        return icon;
    }

    private static UIElement createGlyphBlock(int left, int top, int width, int height) {
        return new UIElement()
                .addClass("oreui-zoom-glyph")
                .layout(layout -> {
                    layout.positionType(YogaPositionType.ABSOLUTE);
                    layout.left(left);
                    layout.top(top);
                    layout.width(width);
                    layout.height(height);
                });
    }

    private static void renderMiniKline(UIElement[] wickElements, UIElement[] bodyElements, int[] candles) {
        renderCandles(wickElements, bodyElements, candles, 11, "oreui-mini-wick", "oreui-mini-body");
    }

    private static int[] clampPopupRect(int x, int y, int width, int height, UIElement root) {
        int rootWidth = Math.max(460, Math.round(root.getContentWidth()));
        int rootHeight = Math.max(220, Math.round(root.getContentHeight()));
        int maxWidth = Math.max(POPUP_MIN_WIDTH, rootWidth - POPUP_MARGIN * 2);
        int maxHeight = Math.max(POPUP_MIN_HEIGHT, rootHeight - POPUP_MARGIN * 2);
        int clampedWidth = Mth.clamp(width, POPUP_MIN_WIDTH, maxWidth);
        int clampedHeight = Mth.clamp(height, POPUP_MIN_HEIGHT, maxHeight);
        int maxX = Math.max(POPUP_MARGIN, rootWidth - clampedWidth - POPUP_MARGIN);
        int maxY = Math.max(POPUP_MARGIN, rootHeight - clampedHeight - POPUP_MARGIN);
        int clampedX = Mth.clamp(x, POPUP_MARGIN, maxX);
        int clampedY = Mth.clamp(y, POPUP_MARGIN, maxY);
        return new int[] { clampedX, clampedY, clampedWidth, clampedHeight };
    }

    private static void applyPopupRect(UIElement popupWindow, int x, int y, int width, int height) {
        popupWindow.layout(layout -> {
            layout.left(x);
            layout.top(y);
            layout.width(width);
            layout.height(height);
        });
    }

    private static void renderCandles(
            UIElement[] wickElements,
            UIElement[] bodyElements,
            int[] candles,
            int chartHeight,
            String wickClass,
            String bodyClass) {
        int candleCount = Math.min(wickElements.length, candles == null ? 0 : candles.length / 4);
        if (candleCount <= 0) {
            for (int i = 0; i < wickElements.length; i++) {
                wickElements[i].setVisible(false);
                bodyElements[i].setVisible(false);
            }
            return;
        }

        int globalHigh = Integer.MIN_VALUE;
        int globalLow = Integer.MAX_VALUE;
        for (int i = 0; i < candleCount; i++) {
            int offset = i * 4;
            globalHigh = Math.max(globalHigh, candles[offset + 1]);
            globalLow = Math.min(globalLow, candles[offset + 2]);
        }
        int priceRange = Math.max(1, globalHigh - globalLow);
        int drawHeight = Math.max(2, chartHeight);

        for (int i = 0; i < wickElements.length; i++) {
            UIElement wick = wickElements[i];
            UIElement body = bodyElements[i];
            if (i >= candleCount) {
                wick.setVisible(false);
                body.setVisible(false);
                continue;
            }

            int offset = i * 4;
            int open = candles[offset];
            int high = candles[offset + 1];
            int low = candles[offset + 2];
            int close = candles[offset + 3];

            int lowY = Math.round((low - globalLow) * drawHeight / (float) priceRange);
            int highY = Math.round((high - globalLow) * drawHeight / (float) priceRange);
            int openY = Math.round((open - globalLow) * drawHeight / (float) priceRange);
            int closeY = Math.round((close - globalLow) * drawHeight / (float) priceRange);

            int wickBottom = Mth.clamp(Math.min(lowY, highY), 0, drawHeight);
            int wickTop = Mth.clamp(Math.max(lowY, highY), 0, drawHeight);
            int bodyBottom = Mth.clamp(Math.min(openY, closeY), 0, drawHeight);
            int bodyTop = Mth.clamp(Math.max(openY, closeY), 0, drawHeight);

            int wickHeight = Math.max(1, wickTop - wickBottom + 1);
            int bodyHeight = Math.max(2, bodyTop - bodyBottom + 1);
            UIElement candle = body.getParent();
            int candleWidth = Math.max(1, candle == null ? Math.round(body.getSizeWidth()) : Math.round(candle.getSizeWidth()));
            int wickWidth = Math.max(1, Math.round(wick.getSizeWidth()));
            int bodyWidth = Math.max(wickWidth + 1, Math.round(body.getSizeWidth()));
            bodyWidth = Math.min(Math.max(1, bodyWidth), candleWidth);
            if ((bodyWidth & 1) == 0 && bodyWidth < candleWidth) {
                bodyWidth++;
            }
            int bodyLeft = Math.max(0, (candleWidth - bodyWidth) / 2);
            int wickLeft = Math.max(0, bodyLeft + (bodyWidth - wickWidth) / 2);
            final int finalWickLeft = wickLeft;
            final int finalWickWidth = wickWidth;
            final int finalBodyLeft = bodyLeft;
            final int finalBodyWidth = bodyWidth;

            wick.layout(layout -> {
                layout.left(finalWickLeft);
                layout.width(finalWickWidth);
                layout.bottom(wickBottom);
                layout.height(wickHeight);
            });
            body.layout(layout -> {
                layout.left(finalBodyLeft);
                layout.width(finalBodyWidth);
                layout.bottom(bodyBottom);
                layout.height(bodyHeight);
            });

            wick.setVisible(true);
            body.setVisible(true);
            wick.setClasses(wickClass);
            body.setClasses(bodyClass);
            if (close > open) {
                wick.addClass("oreui-candle-up");
                body.addClass("oreui-candle-up");
            } else if (close < open) {
                wick.addClass("oreui-candle-down");
                body.addClass("oreui-candle-down");
            }
        }
    }

    private static final class PopupTrendLineOverlay extends UIElement {
        private static final int TREND_COLOR = 0xFF8EC6FF;
        private final AtomicReference<int[]> candlesRef;

        private PopupTrendLineOverlay(AtomicReference<int[]> candlesRef) {
            this.candlesRef = candlesRef;
            setAllowHitTest(false);
        }

        @Override
        public void drawBackgroundAdditional(GUIContext guiContext) {
            super.drawBackgroundAdditional(guiContext);
            int[] candles = candlesRef.get();
            int candleCount = candles == null ? 0 : candles.length / 4;
            UIElement anchor = getParent() == null ? this : getParent();
            int chartWidth = Math.max(Math.round(getContentWidth()), Math.round(anchor.getContentWidth()));
            int chartHeight = Math.max(Math.round(getContentHeight()), Math.round(anchor.getContentHeight()));
            if (candleCount <= 1 || chartWidth <= 1 || chartHeight <= 1) {
                return;
            }

            int globalHigh = Integer.MIN_VALUE;
            int globalLow = Integer.MAX_VALUE;
            for (int i = 0; i < candleCount; i++) {
                int offset = i * 4;
                globalHigh = Math.max(globalHigh, candles[offset + 1]);
                globalLow = Math.min(globalLow, candles[offset + 2]);
            }
            int priceRange = Math.max(1, globalHigh - globalLow);
            int drawWidth = Math.max(2, chartWidth);
            int drawHeight = Math.max(2, chartHeight);
            float originX = anchor.getContentX();
            float originY = anchor.getContentY();
            int previousX = 0;
            int previousY = 0;
            for (int i = 0; i < candleCount; i++) {
                int offset = i * 4;
                int close = candles[offset + 3];
                int closeY = Math.round((close - globalLow) * (drawHeight - 1) / (float) priceRange);
                int pointX = Mth.clamp(Math.round((i + 0.5F) * drawWidth / candleCount), 0, drawWidth - 1);
                int pointY = Mth.clamp(closeY, 0, drawHeight - 1);
                if (i > 0) {
                    drawTrendSegment(guiContext, originX, originY, drawHeight, previousX, previousY, pointX, pointY);
                }
                previousX = pointX;
                previousY = pointY;
            }
        }

        private static void drawTrendSegment(
                GUIContext guiContext,
                float originX,
                float originY,
                int drawHeight,
                int x0,
                int y0,
                int x1,
                int y1) {
            int currentX = x0;
            int currentY = y0;
            int deltaX = Math.abs(x1 - x0);
            int stepX = x0 < x1 ? 1 : -1;
            int deltaY = -Math.abs(y1 - y0);
            int stepY = y0 < y1 ? 1 : -1;
            int error = deltaX + deltaY;
            while (true) {
                float drawX = originX + currentX;
                float drawY = originY + (drawHeight - 1 - currentY);
                DrawerHelper.drawSolidRect(guiContext.graphics, drawX, drawY, 1, 1, TREND_COLOR);
                if (currentX == x1 && currentY == y1) {
                    break;
                }
                int twiceError = error << 1;
                if (twiceError >= deltaY) {
                    error += deltaY;
                    currentX += stepX;
                }
                if (twiceError <= deltaX) {
                    error += deltaX;
                    currentY += stepY;
                }
            }
        }
    }

    private static String formatSpurs(int amount) {
        return String.format(Locale.ROOT, "%,d sp", Math.max(0, amount));
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%+.2f%%", value);
    }

    private static ChatFormatting colorForChange(double value) {
        if (value > 0.01D) {
            return ChatFormatting.RED;
        }
        if (value < -0.01D) {
            return ChatFormatting.GREEN;
        }
        return ChatFormatting.YELLOW;
    }

    private static <T extends UIElement> T withPressTextShift(T element) {
        element.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                event.currentElement.addClass("__oreui_btn_pressed__");
            }
        });
        element.addEventListener(UIEvents.MOUSE_UP, event -> event.currentElement.removeClass("__oreui_btn_pressed__"));
        element.addEventListener(UIEvents.MOUSE_LEAVE, event -> event.currentElement.removeClass("__oreui_btn_pressed__"),
                true);
        return element;
    }

    @FunctionalInterface
    public interface ActionDispatcher {
        void send(StockUiAction action, int targetStock);
    }

    public record StockUiView(ModularUI modularUI, Consumer<StockUiSnapshot> applySnapshot) {
    }
}

