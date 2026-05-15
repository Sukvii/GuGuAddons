package com.gugucraft.guguaddons.client.compat.emi;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.recipe.AbyssCatalysisRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;

public class AbyssCatalysisEmiRecipe extends BasicEmiRecipe {
    private static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID,
            "textures/gui/jei/abyss_catalysis_widgets.png");
    private static final int WIDTH = 193;
    private static final int HEIGHT = 163;
    private static final int SLOT_STEP = 19;
    private static final int LEFT_X = 8;
    private static final int RIGHT_X = 131;
    private static final int BOTTOM_INPUT_Y = 101;
    private static final int CATALYST_Y = 83;
    private static final int OUTPUT_Y = 121;
    private static final int LEFT_COLUMNS = 4;
    private static final int RIGHT_COLUMNS = 3;
    private static final int INPUT_MAX_SLOTS = 8;
    private static final int CATALYST_MAX_SLOTS = 6;
    private static final int OUTPUT_MAX_SLOTS = 6;
    private static final AnimatedAbyssCatalyticChamberDisplay MACHINE_DISPLAY = new AnimatedAbyssCatalyticChamberDisplay();

    private final AbyssCatalysisRecipe recipe;
    private final List<DisplayEntry> topInputs;
    private final List<DisplayEntry> catalystInputs;
    private final List<DisplayEntry> bottomInputs;
    private final List<DisplayEntry> outputEntries;

    public AbyssCatalysisEmiRecipe(EmiRecipeCategory category, ResourceLocation id, AbyssCatalysisRecipe recipe) {
        super(category, id, WIDTH, HEIGHT);
        this.recipe = recipe;
        this.topInputs = collectInputs(recipe.getTopItemIngredients(), recipe.getTopFluidIngredients());
        this.catalystInputs = collectInputs(recipe.getCatalystItemIngredients(), recipe.getCatalystFluidIngredients());
        this.bottomInputs = collectInputs(recipe.getBottomItemIngredients(), recipe.getBottomFluidIngredients());
        this.outputEntries = collectOutputs();

        addInputs(topInputs);
        addInputs(catalystInputs);
        addInputs(bottomInputs);
        for (ProcessingOutput output : recipe.getRollableResults()) {
            outputs.add(EmiStack.of(output.getStack()).setChance(output.getChance()));
        }
        for (FluidStack output : recipe.getFluidResults()) {
            outputs.add(NeoForgeEmiStack.of(output));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, WIDTH, HEIGHT, (draw, mouseX, mouseY, delta) -> drawBackground(draw));

        int bottomRows = visibleRows(bottomInputs, LEFT_COLUMNS, INPUT_MAX_SLOTS);
        int topOffset = -40 + (bottomRows > 1 ? -SLOT_STEP : 0);
        int outputRows = visibleRows(outputEntries, RIGHT_COLUMNS, OUTPUT_MAX_SLOTS);

        addCompactGroup(widgets, bottomInputs, LEFT_X, BOTTOM_INPUT_Y, LEFT_COLUMNS, INPUT_MAX_SLOTS, false,
                true, false, Component.translatable("guguaddons.emi.abyss_catalysis.bottom"));
        addCompactGroup(widgets, topInputs, LEFT_X, BOTTOM_INPUT_Y + topOffset, LEFT_COLUMNS, INPUT_MAX_SLOTS, false,
                true, false, Component.translatable("guguaddons.emi.abyss_catalysis.top"));
        addCompactGroup(widgets, outputEntries, RIGHT_X, OUTPUT_Y, RIGHT_COLUMNS, OUTPUT_MAX_SLOTS, false,
                false, true, Component.translatable("guguaddons.emi.abyss_catalysis.results"));
        addCompactGroup(widgets, catalystInputs, RIGHT_X, CATALYST_Y + (outputRows > 1 ? -23 : 0),
                RIGHT_COLUMNS, CATALYST_MAX_SLOTS, true, false, false,
                Component.translatable("guguaddons.emi.abyss_catalysis.catalysts_short"));

        addHeatCatalysts(widgets);
        widgets.addTooltipText(machineTooltip(), 72, 20, 50, 118);
    }

    private void addInputs(List<DisplayEntry> entries) {
        entries.stream().map(DisplayEntry::ingredient).forEach(inputs::add);
    }

    private List<DisplayEntry> collectInputs(List<Ingredient> itemIngredients,
            List<SizedFluidIngredient> fluidIngredients) {
        List<DisplayEntry> entries = new ArrayList<>(itemIngredients.size() + fluidIngredients.size());
        for (Ingredient ingredient : itemIngredients) {
            addMerged(entries, DisplayEntry.slot(EmiIngredient.of(ingredient), false));
        }
        for (SizedFluidIngredient ingredient : fluidIngredients) {
            EmiIngredient emiIngredient = NeoForgeEmiIngredient.of(ingredient);
            addMerged(entries, DisplayEntry.tank(emiIngredient, Math.max((int) emiIngredient.getAmount(), 1000), false));
        }
        return entries;
    }

    private List<DisplayEntry> collectOutputs() {
        List<DisplayEntry> entries = new ArrayList<>(recipe.getRollableResults().size() + recipe.getFluidResults().size());
        for (ProcessingOutput output : recipe.getRollableResults()) {
            entries.add(DisplayEntry.slot(EmiStack.of(output.getStack()).setChance(output.getChance()), true));
        }
        for (FluidStack output : recipe.getFluidResults()) {
            entries.add(DisplayEntry.tank(NeoForgeEmiStack.of(output), Math.max(output.getAmount(), 1000), true));
        }
        return entries;
    }

    private void addCompactGroup(WidgetHolder widgets, List<DisplayEntry> entries, int x, int y, int columns,
            int maxSlots, boolean catalysts, boolean rightAlignSmall, boolean centerSmall, Component sectionTooltip) {
        if (entries.isEmpty()) {
            return;
        }

        boolean collapsed = entries.size() > maxSlots;
        int displaySize = collapsed ? maxSlots : entries.size();
        int visibleEntries = collapsed ? maxSlots - 1 : entries.size();
        for (int index = 0; index < visibleEntries; index++) {
            addEntry(widgets, entries.get(index), x, y, columns, index, displaySize, catalysts, rightAlignSmall,
                    centerSmall, sectionTooltip);
        }

        if (!collapsed) {
            return;
        }

        int overflowIndex = visibleEntries;
        int overflowX = slotX(x, columns, overflowIndex, displaySize, rightAlignSmall, centerSmall);
        int overflowY = slotY(y, columns, overflowIndex);
        int hiddenCount = entries.size() - visibleEntries;
        List<EmiIngredient> hidden = entries.subList(visibleEntries, entries.size()).stream()
                .map(DisplayEntry::ingredient)
                .toList();
        boolean chance = entries.subList(visibleEntries, entries.size()).stream()
                .anyMatch(entry -> entry.output() && entry.ingredient().getChance() != 1F);

        addCreateSlotBackground(widgets, overflowX, overflowY, chance);
        SlotWidget slot = widgets.addSlot(EmiIngredient.of(hidden), overflowX, overflowY)
                .appendTooltip(sectionTooltip)
                .appendTooltip(Component.translatable("guguaddons.emi.abyss_catalysis.collapsed", hiddenCount));
        slot.drawBack(false);
        if (catalysts) {
            appendStochasticTooltip(slot, recipe.getCatalystConsumptionChance());
        }
        if (entries.get(visibleEntries).output()) {
            slot.recipeContext(this);
            appendStochasticTooltip(slot, entries.get(visibleEntries).ingredient().getChance());
        }
        addOverflowOverlay(widgets, overflowX, overflowY, hiddenCount);
    }

    private void addEntry(WidgetHolder widgets, DisplayEntry entry, int x, int y, int columns, int index,
            int displaySize, boolean catalyst, boolean rightAlignSmall, boolean centerSmall, Component sectionTooltip) {
        int slotX = slotX(x, columns, index, displaySize, rightAlignSmall, centerSmall);
        int slotY = slotY(y, columns, index);
        addCreateSlotBackground(widgets, slotX, slotY, entry.output() && entry.ingredient().getChance() != 1F);
        SlotWidget slot = entry.tank()
                ? widgets.addTank(entry.ingredient(), slotX, slotY, 18, 18, entry.capacity())
                : widgets.addSlot(entry.ingredient(), slotX, slotY);
        slot.drawBack(false);
        slot.appendTooltip(sectionTooltip);
        if (entry.output()) {
            slot.recipeContext(this);
            appendStochasticTooltip(slot, entry.ingredient().getChance());
        }
        if (catalyst) {
            appendStochasticTooltip(slot, recipe.getCatalystConsumptionChance());
        }
    }

    private void addOverflowOverlay(WidgetHolder widgets, int x, int y, int hiddenCount) {
        widgets.addDrawable(x, y, 18, 18, (draw, mouseX, mouseY, delta) -> {
            String text = "+" + hiddenCount;
            int width = Minecraft.getInstance().font.width(text);
            draw.fill(1, 10, 17, 18, 0xAA000000);
            draw.drawString(Minecraft.getInstance().font, text, 17 - width, 10, 0xFFFFFF, false);
        });
    }

    private void drawBackground(GuiGraphics draw) {
        HeatCondition requiredHeat = recipe.getRequiredHeat();
        int outputRows = visibleRows(outputEntries, RIGHT_COLUMNS, OUTPUT_MAX_SLOTS);
        if (outputRows <= 1) {
            AllGuiTextures.JEI_DOWN_ARROW.render(draw, 144, 104);
        } else {
            draw.blit(WIDGETS, 125, 80, 19, 21, 17, 15);
        }

        boolean noHeat = requiredHeat == HeatCondition.NONE;
        AllGuiTextures shadow = noHeat ? AllGuiTextures.JEI_SHADOW : AllGuiTextures.JEI_LIGHT;
        shadow.render(draw, 81, 108 + (noHeat ? 10 : 30));
        draw.blit(WIDGETS, 12, 140, 0, noHeat ? 221 : 201, 169, 19);
        draw.drawString(Minecraft.getInstance().font, CreateLang.translateDirect(requiredHeat.getTranslationKey()),
                17, 146, requiredHeat.getColor(), false);

        if (requiredHeat != HeatCondition.NONE) {
            MACHINE_DISPLAY.drawHeater(draw, WIDTH / 2 - 5, 105, requiredHeat.visualizeAsBlazeBurner());
        }
        MACHINE_DISPLAY.drawMachine(draw, WIDTH / 2 - 5, 32);
    }

    private List<Component> machineTooltip() {
        return List.of(
                Component.translatable("guguaddons.emi.abyss_catalysis.machine"),
                Component.translatable("guguaddons.emi.abyss_catalysis.heat",
                        CreateLang.translateDirect(recipe.getRequiredHeat().getTranslationKey()))
        );
    }

    private static int slotX(int startX, int columns, int slotIndex, int displaySize, boolean rightAlignSmall,
            boolean centerSmall) {
        int offset = 0;
        if (displaySize < columns) {
            if (rightAlignSmall) {
                offset = (columns - displaySize) * SLOT_STEP;
            } else if (centerSmall) {
                offset = (columns - displaySize) * SLOT_STEP / 2;
            }
        }
        return startX + offset + slotIndex % columns * SLOT_STEP;
    }

    private static int slotY(int startY, int columns, int slotIndex) {
        return startY - slotIndex / columns * SLOT_STEP;
    }

    private static int visibleRows(List<DisplayEntry> entries, int columns, int maxSlots) {
        int visible = entries.isEmpty() ? 1 : Math.min(entries.size(), maxSlots);
        return (visible - 1) / columns + 1;
    }

    private static void addCreateSlotBackground(WidgetHolder widgets, int x, int y, boolean chance) {
        AllGuiTextures texture = chance ? AllGuiTextures.JEI_CHANCE_SLOT : AllGuiTextures.JEI_SLOT;
        widgets.addDrawable(x, y, 18, 18, (draw, mouseX, mouseY, delta) -> texture.render(draw, 0, 0));
    }

    private void addHeatCatalysts(WidgetHolder widgets) {
        HeatCondition requiredHeat = recipe.getRequiredHeat();
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.NONE)) {
            widgets.addSlot(EmiStack.of(AllBlocks.BLAZE_BURNER.get()), 134, 141);
        }
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.KINDLED)) {
            widgets.addSlot(EmiStack.of(AllItems.BLAZE_CAKE.get()), 153, 141);
        }
    }

    private static void appendStochasticTooltip(SlotWidget slot, float chance) {
        if (chance == 1F) {
            return;
        }
        slot.appendTooltip(stochasticTooltip(chance));
    }

    private static Component stochasticTooltip(float chance) {
        Component component = chance == 0F
                ? CreateLang.translateDirect("recipe.deploying.not_consumed")
                : CreateLang.translateDirect("recipe.processing.chance",
                        chance < 0.01F ? "<1" : (int) (chance * 100F));
        return component.copy().withStyle(ChatFormatting.GOLD);
    }

    private static void addMerged(List<DisplayEntry> entries, DisplayEntry next) {
        for (int index = 0; index < entries.size(); index++) {
            DisplayEntry existing = entries.get(index);
            if (existing.tank() != next.tank() || !EmiIngredient.areEqual(existing.ingredient(), next.ingredient())) {
                continue;
            }

            long amount = existing.ingredient().getAmount() + next.ingredient().getAmount();
            EmiIngredient merged = existing.ingredient().copy().setAmount(amount);
            int capacity = existing.tank() ? Math.max((int) Math.min(Integer.MAX_VALUE, amount),
                    Math.max(existing.capacity(), next.capacity())) : 0;
            entries.set(index, new DisplayEntry(merged, existing.tank(), capacity, existing.output()));
            return;
        }
        entries.add(next);
    }

    private record DisplayEntry(EmiIngredient ingredient, boolean tank, int capacity, boolean output) {
        private static DisplayEntry slot(EmiIngredient ingredient, boolean output) {
            return new DisplayEntry(ingredient, false, 0, output);
        }

        private static DisplayEntry tank(EmiIngredient ingredient, int capacity, boolean output) {
            return new DisplayEntry(ingredient, true, capacity, output);
        }
    }
}
