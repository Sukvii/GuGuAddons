package com.gugucraft.guguaddons.client.ldlib;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class LDLibUiFactory {
    private static final int MAX_LOT = 100;
    private static final int STEP_SMALL = 1;
    private static final int STEP_LARGE = 10;

    private LDLibUiFactory() {
    }

    public static ModularUI createDemoUi(Player player) {
        AtomicInteger lotValue = new AtomicInteger(10);

        Stylesheet oreUiStyles = Stylesheet.parse("""
                #oreui_root {
                  background: sdf(#151922, 1, 1, #252c3b);
                  padding-all: 10;
                  gap-all: 5;
                }

                #oreui_root text, #oreui_root label {
                  text-shadow: false;
                }

                #oreui_shell {
                  background: sdf(#2a303b, 1, 1, #5b6575);
                  padding-all: 6;
                  gap-all: 5;
                }

                .oreui-title-panel {
                  background: sdf(#343b48, 1, 1, #707a8a);
                  padding-all: 5;
                  gap-all: 2;
                }

                #oreui_title {
                  text-color: #f1f4fa;
                  font-size: 13;
                  horizontal-align: left;
                }

                #oreui_hint {
                  text-color: #adb7c8;
                  text-wrap: wrap;
                  adaptive-height: true;
                }

                .oreui-surface {
                  background: sdf(#303643, 1, 1, #667181);
                  padding-all: 4;
                  gap-all: 3;
                }

                .oreui-row {
                  flex-direction: row;
                  gap-column: 4;
                }

                .oreui-row button:host {
                  flex: 1;
                }

                .oreui-list-item {
                  background: sdf(#3a414f, 1, 1, #768294);
                  flex-direction: row;
                  padding-horizontal: 4;
                  padding-vertical: 2;
                  gap-column: 4;
                }

                #oreui_key_lot, #oreui_key_progress {
                  text-color: #d8deea;
                }

                #oreui_value_lot, #oreui_value_progress {
                  text-color: #f1f4fb;
                  horizontal-align: right;
                }

                .oreui-meter-frame {
                  background: sdf(#1f2530, 1, 1, #6f7a8b);
                  padding-all: 2;
                }

                button:host {
                  base-background: sdf(#383f4c, 1, 1, #8a95a5);
                  hover-background: sdf(#464f5e, 1, 1, #a8b1bf);
                  pressed-background: sdf(#2f3540, 1, 1, #768093) translate(0, 0.9);
                  padding-all: 3;
                  height: 17;
                }

                button:host .__button_text__ {
                  text-color: #eef2f8;
                }

                button.__oreui_btn_pressed__ .__button_text__ {
                  transform: translateY(0.9);
                }

                .oreui-btn-neutral {
                  base-background: sdf(#3b4351, 1, 1, #8f9aac);
                  hover-background: sdf(#4a5464, 1, 1, #aab4c4);
                  pressed-background: sdf(#313844, 1, 1, #7a8598) translate(0, 0.9);
                }

                .oreui-btn-primary {
                  base-background: sdf(#4da93a, 1, 1, #2f6f24);
                  hover-background: sdf(#60c347, 1, 1, #3a8530);
                  pressed-background: sdf(#428f32, 1, 1, #2b6122) translate(0, 0.9);
                }

                .oreui-btn-primary .__button_text__ {
                  text-color: #f2fff0;
                }

                progress-bar:host {
                  height: 7;
                }

                progress-bar:host label {
                  text-color: #dbe4f4;
                  font-size: 6;
                }

                .__progress-bar_bar-container__ {
                  background: sdf(#2b3340, 1, 1, #6e798a);
                  padding-all: 0;
                }

                .__progress-bar_bar__ {
                  background: sdf(#4da93a, 1);
                  padding-all: 0;
                }
                """);

        UIElement root = new UIElement()
                .setId("oreui_root")
                .layout(layout -> layout.width(254))
                .addChildren(
                        new UIElement()
                                .setId("oreui_shell")
                                .addChildren(
                                        new UIElement()
                                                .addClass("oreui-title-panel")
                                                .addChildren(
                                                        new Label().setText(Component.translatable("screen.guguaddons.ldlib.title"))
                                                                .setId("oreui_title"),
                                                        new Label().setText(Component.translatable("screen.guguaddons.ldlib.hint"))
                                                                .setId("oreui_hint")),
                                        new UIElement()
                                                .addClass("oreui-surface")
                                                .addChildren(
                                                        new UIElement()
                                                                .addClass("oreui-list-item")
                                                                .addChildren(
                                                                        new Label().setText(Component.literal("LOT SIZE"))
                                                                                .setId("oreui_key_lot")
                                                                                .layout(layout -> layout.flex(1)),
                                                                        new Label()
                                                                                .bindDataSource(SupplierDataSource.of(
                                                                                        () -> Component.literal(String.format(Locale.ROOT, "%d", lotValue.get()))))
                                                                                .setId("oreui_value_lot")
                                                                                .layout(layout -> layout.width(70))),
                                                        new UIElement()
                                                                .addClass("oreui-list-item")
                                                                .addChildren(
                                                                        new Label().setText(Component.literal("PROGRESS"))
                                                                                .setId("oreui_key_progress")
                                                                                .layout(layout -> layout.flex(1)),
                                                                        new Label()
                                                                                .bindDataSource(SupplierDataSource.of(
                                                                                        () -> Component.literal(String.format(Locale.ROOT, "%d%%", lotValue.get() * 100 / MAX_LOT))))
                                                                                .setId("oreui_value_progress")
                                                                                .layout(layout -> layout.width(70))),
                                                        new UIElement()
                                                                .addClass("oreui-meter-frame")
                                                                .addChild(new ProgressBar()
                                                                        .bindDataSource(SupplierDataSource.of(
                                                                                () -> lotValue.get() / (float) MAX_LOT))
                                                                        .label(label -> label.setDisplay(false)))),
                                        new UIElement()
                                                .addClasses("oreui-surface", "oreui-row")
                                                .addChildren(
                                                        withPressTextShift(new Button()
                                                                .setText(Component.translatable("screen.guguaddons.ldlib.button.minus1"))
                                                                .setOnClick(event -> changeValue(lotValue, -STEP_SMALL))
                                                                .addClass("oreui-btn-neutral")),
                                                        withPressTextShift(new Button()
                                                                .setText(Component.translatable("screen.guguaddons.ldlib.button.plus1"))
                                                                .setOnClick(event -> changeValue(lotValue, STEP_SMALL))
                                                                .addClass("oreui-btn-neutral")),
                                                        withPressTextShift(new Button()
                                                                .setText(Component.translatable("screen.guguaddons.ldlib.button.plus10"))
                                                                .setOnClick(event -> changeValue(lotValue, STEP_LARGE))
                                                                .addClass("oreui-btn-primary"))),
                                        new UIElement()
                                                .addClasses("oreui-surface", "oreui-row")
                                                .addChildren(
                                                        withPressTextShift(new Button()
                                                                .setText(Component.translatable("screen.guguaddons.ldlib.button.reset"))
                                                                .setOnClick(event -> lotValue.set(10))
                                                                .addClass("oreui-btn-neutral")),
                                                        withPressTextShift(new Button()
                                                                .setText(Component.translatable("screen.guguaddons.ldlib.button.close"))
                                                                .setOnClick(event -> {
                                                                    var modularUI = event.currentElement.getModularUI();
                                                                    if (modularUI != null && modularUI.getScreen() != null) {
                                                                        modularUI.getScreen().onClose();
                                                                    }
                                                                })
                                                                .addClass("oreui-btn-neutral")))));

        return ModularUI.of(UI.of(root, oreUiStyles), player);
    }

    private static void changeValue(AtomicInteger value, int delta) {
        value.set(Mth.clamp(value.get() + delta, 0, MAX_LOT));
    }

    private static UIElement withPressTextShift(UIElement element) {
        element.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                event.currentElement.addClass("__oreui_btn_pressed__");
            }
        });
        element.addEventListener(UIEvents.MOUSE_UP, event -> event.currentElement.removeClass("__oreui_btn_pressed__"));
        element.addEventListener(UIEvents.MOUSE_LEAVE, event -> event.currentElement.removeClass("__oreui_btn_pressed__"), true);
        return element;
    }
}
