package com.gugucraft.guguaddons.client.compat.emi;

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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;

public class AbyssCatalysisEmiRecipe extends BasicEmiRecipe {
    // Layout follows Create's basin categories (BasinCategory): 177 wide, 19 px slot pitch, Create slot textures,
    // the animated machine in the middle and Create's heat bar along the bottom.
    private static final int WIDTH = 177;
    private static final int HEIGHT = 124;
    private static final int SLOT_STEP = 19;

    // Process strip: one column per step in process order (I settle / II activate / III condense), which feed the
    // bottom / middle / top tier of the chamber respectively. Like Create's sequenced assembly, each column only
    // shows a grey roman numeral; the step name, tier, frequency and rules are in the numeral's tooltip.
    private static final int COLUMN_WIDTH = 59;
    private static final int NUMERAL_Y = 2;
    private static final int STAGE_SLOT_Y = 12;
    private static final int STAGE_MAX_SLOTS = 3;
    private static final String[] NUMERALS = {"I", "II", "III"};
    private static final int NUMERAL_COLOR = 0x888888;

    // Machine: shrieker on top of the three tiers, blaze burner below; centred near x 88 like Create's basin.
    private static final int MACHINE_SCALE = 15;
    private static final int MACHINE_X = 78;
    private static final int MACHINE_Y = 54;
    private static final int MACHINE_CENTER_X = MACHINE_X + Math.round(MACHINE_SCALE * 0.65F);
    private static final int MACHINE_RIGHT = MACHINE_X + Math.round(MACHINE_SCALE * 1.31F);
    private static final int MACHINE_TOP = MACHINE_Y - MACHINE_SCALE - Math.round(MACHINE_SCALE * 0.27F);
    private static final int MACHINE_BOTTOM = MACHINE_Y + 3 * MACHINE_SCALE;
    private static final int MACHINE_MIDDLE_Y = MACHINE_Y + MACHINE_SCALE * 3 / 2;
    private static final int HEATER_Y = MACHINE_Y + 2 * MACHINE_SCALE + Math.round(MACHINE_SCALE * 0.17F);

    private static final int OUTPUT_X = 140;
    private static final int OUTPUT_COLUMNS = 2;
    private static final int OUTPUT_MAX_SLOTS = 4;
    private static final int HEAT_BAR_Y = HEIGHT - 20;

    private static final AnimatedAbyssCatalyticChamberDisplay MACHINE_DISPLAY = new AnimatedAbyssCatalyticChamberDisplay();

    private final RecipeHolder<?> backingRecipe;
    private final AbyssCatalysisRecipe recipe;
    private final List<DisplayEntry> topInputs;
    private final List<DisplayEntry> catalystInputs;
    private final List<DisplayEntry> bottomInputs;
    private final List<DisplayEntry> outputEntries;

    public AbyssCatalysisEmiRecipe(EmiRecipeCategory category, RecipeHolder<AbyssCatalysisRecipe> holder) {
        super(category, holder.id(), WIDTH, HEIGHT);
        this.backingRecipe = holder;
        this.recipe = holder.value();
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
    public RecipeHolder<?> getBackingRecipe() {
        return backingRecipe;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, WIDTH, HEIGHT, (draw, mouseX, mouseY, delta) -> drawBackground(draw));

        addStage(widgets, 0, bottomInputs, false, Component.translatable("guguaddons.emi.abyss_catalysis.bottom"),
                stageTooltip(1, Component.translatable("guguaddons.emi.abyss_catalysis.timing")));
        addStage(widgets, 1, catalystInputs, true, Component.translatable("guguaddons.emi.abyss_catalysis.catalysts_short"),
                stageTooltip(2, Component.translatable("guguaddons.emi.abyss_catalysis.timing"),
                        Component.translatable("guguaddons.emi.abyss_catalysis.catalyst_chance",
                                percent(recipe.getCatalystConsumptionChance())),
                        Component.translatable("guguaddons.emi.abyss_catalysis.stage_2.waiting")));
        addStage(widgets, 2, topInputs, false, Component.translatable("guguaddons.emi.abyss_catalysis.top"),
                stageTooltip(3, Component.translatable("guguaddons.emi.abyss_catalysis.stage_3.waiting"),
                        Component.translatable("guguaddons.emi.abyss_catalysis.failure").withStyle(ChatFormatting.RED)));
        addOutputs(widgets);
        addHeatCatalysts(widgets);
        widgets.addTooltipText(machineTooltip(), MACHINE_X - 2, MACHINE_TOP, MACHINE_RIGHT - MACHINE_X + 4,
                MACHINE_BOTTOM - MACHINE_TOP);
    }

    // ---------------------------------------------------------------- process strip

    private void addStage(WidgetHolder widgets, int column, List<DisplayEntry> entries, boolean catalyst,
            Component sectionTooltip, List<Component> titleTooltip) {
        int columnX = column * COLUMN_WIDTH;
        widgets.addTooltipText(titleTooltip, columnX, NUMERAL_Y - 1, COLUMN_WIDTH, 10);
        if (entries.isEmpty()) {
            return;
        }
        int shown = Math.min(entries.size(), STAGE_MAX_SLOTS);
        int startX = columnX + (COLUMN_WIDTH - (shown * SLOT_STEP - 1)) / 2;
        addSlots(widgets, entries, startX, STAGE_SLOT_Y, STAGE_MAX_SLOTS, STAGE_MAX_SLOTS, catalyst, sectionTooltip);
    }

    private static List<Component> stageTooltip(int stage, Component... lines) {
        List<Component> tooltip = new ArrayList<>(lines.length + 2);
        tooltip.add(Component.translatable("guguaddons.emi.abyss_catalysis.stage_" + stage));
        tooltip.add(Component.translatable("guguaddons.emi.abyss_catalysis.stage_" + stage + ".tier")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("guguaddons.emi.abyss_catalysis.stage_" + stage + ".frequency")
                .withStyle(ChatFormatting.GRAY));
        tooltip.addAll(List.of(lines));
        return tooltip;
    }

    // ---------------------------------------------------------------- outputs

    private void addOutputs(WidgetHolder widgets) {
        if (outputEntries.isEmpty()) {
            return;
        }
        int shown = Math.min(outputEntries.size(), OUTPUT_MAX_SLOTS);
        int columns = Math.min(shown, OUTPUT_COLUMNS);
        int rows = (shown + OUTPUT_COLUMNS - 1) / OUTPUT_COLUMNS;
        addSlots(widgets, outputEntries, outputStartX(), MACHINE_MIDDLE_Y - (rows * SLOT_STEP - 1) / 2, columns,
                OUTPUT_MAX_SLOTS, false, Component.translatable("guguaddons.emi.abyss_catalysis.results"));
    }

    private int outputStartX() {
        int columns = Math.min(Math.min(outputEntries.size(), OUTPUT_MAX_SLOTS), OUTPUT_COLUMNS);
        return OUTPUT_X + (WIDTH - OUTPUT_X - (columns * SLOT_STEP - 1)) / 2;
    }

    // ---------------------------------------------------------------- slots

    private void addSlots(WidgetHolder widgets, List<DisplayEntry> entries, int startX, int startY, int columns,
            int maxSlots, boolean catalyst, Component sectionTooltip) {
        boolean collapsed = entries.size() > maxSlots;
        int visible = collapsed ? maxSlots - 1 : entries.size();
        for (int index = 0; index < visible; index++) {
            addEntry(widgets, entries.get(index), slotX(startX, columns, index), slotY(startY, columns, index),
                    catalyst, sectionTooltip);
        }
        if (!collapsed) {
            return;
        }

        int x = slotX(startX, columns, visible);
        int y = slotY(startY, columns, visible);
        List<DisplayEntry> hiddenEntries = entries.subList(visible, entries.size());
        int hiddenCount = hiddenEntries.size();
        boolean output = hiddenEntries.get(0).output();
        boolean chance = hiddenEntries.stream().anyMatch(entry -> entry.output() && entry.ingredient().getChance() != 1F);
        addSlotBackground(widgets, x, y, slotTexture(catalyst, chance));
        SlotWidget slot = widgets.addSlot(EmiIngredient.of(hiddenEntries.stream().map(DisplayEntry::ingredient).toList()), x, y)
                .appendTooltip(sectionTooltip)
                .appendTooltip(Component.translatable("guguaddons.emi.abyss_catalysis.collapsed", hiddenCount));
        slot.drawBack(false);
        if (catalyst) {
            slot.appendTooltip(catalystTooltip(recipe.getCatalystConsumptionChance()));
        }
        if (output) {
            slot.recipeContext(this);
            appendStochasticTooltip(slot, hiddenEntries.get(0).ingredient().getChance());
        }
        addOverflowOverlay(widgets, x, y, hiddenCount);
    }

    private void addEntry(WidgetHolder widgets, DisplayEntry entry, int x, int y, boolean catalyst,
            Component sectionTooltip) {
        addSlotBackground(widgets, x, y, slotTexture(catalyst, entry.output() && entry.ingredient().getChance() != 1F));
        SlotWidget slot = entry.tank()
                ? widgets.addTank(entry.ingredient(), x, y, 18, 18, entry.capacity())
                : widgets.addSlot(entry.ingredient(), x, y);
        slot.drawBack(false);
        slot.appendTooltip(sectionTooltip);
        if (entry.output()) {
            slot.recipeContext(this);
            appendStochasticTooltip(slot, entry.ingredient().getChance());
        }
        if (catalyst) {
            slot.appendTooltip(catalystTooltip(recipe.getCatalystConsumptionChance()));
        }
    }

    /** Catalysts stay in the middle tier; spell out that the chance is the chance of being consumed per batch. */
    private static Component catalystTooltip(float chance) {
        Component component = chance == 0F
                ? CreateLang.translateDirect("recipe.deploying.not_consumed")
                : Component.translatable("guguaddons.emi.abyss_catalysis.catalyst_consume", percent(chance));
        return component.copy().withStyle(ChatFormatting.GOLD);
    }

    private static AllGuiTextures slotTexture(boolean catalyst, boolean chance) {
        if (catalyst) {
            return AllGuiTextures.JEI_CATALYST_SLOT;
        }
        return chance ? AllGuiTextures.JEI_CHANCE_SLOT : AllGuiTextures.JEI_SLOT;
    }

    private static void addSlotBackground(WidgetHolder widgets, int x, int y, AllGuiTextures texture) {
        widgets.addDrawable(x, y, 18, 18, (draw, mouseX, mouseY, delta) -> texture.render(draw, 0, 0));
    }

    private void addOverflowOverlay(WidgetHolder widgets, int x, int y, int hiddenCount) {
        widgets.addDrawable(x, y, 18, 18, (draw, mouseX, mouseY, delta) -> {
            String text = "+" + hiddenCount;
            int width = Minecraft.getInstance().font.width(text);
            draw.fill(1, 10, 17, 18, 0xAA000000);
            draw.drawString(Minecraft.getInstance().font, text, 17 - width, 10, 0xFFFFFF, false);
        });
    }

    private static int slotX(int startX, int columns, int index) {
        return startX + index % columns * SLOT_STEP;
    }

    private static int slotY(int startY, int columns, int index) {
        return startY + index / columns * SLOT_STEP;
    }

    // ---------------------------------------------------------------- background

    private void drawBackground(GuiGraphics draw) {
        Font font = Minecraft.getInstance().font;
        HeatCondition requiredHeat = recipe.getRequiredHeat();
        boolean noHeat = requiredHeat == HeatCondition.NONE;

        // Process strip: grey roman numerals centred over each column (Create's sequenced assembly style)
        for (int column = 0; column < NUMERALS.length; column++) {
            Component numeral = Component.literal(NUMERALS[column]);
            draw.drawString(font, numeral, column * COLUMN_WIDTH + (COLUMN_WIDTH - font.width(numeral)) / 2,
                    NUMERAL_Y, NUMERAL_COLOR, false);
        }

        // Shadow / glow under the machine and Create's heat bar (same order and offsets as BasinCategory)
        if (noHeat) {
            AllGuiTextures.JEI_SHADOW.render(draw, MACHINE_CENTER_X - 26, MACHINE_BOTTOM - 4);
        } else {
            AllGuiTextures.JEI_LIGHT.render(draw, MACHINE_CENTER_X - 26, HEAT_BAR_Y + 8);
        }
        (noHeat ? AllGuiTextures.JEI_NO_HEAT_BAR : AllGuiTextures.JEI_HEAT_BAR).render(draw, 4, HEAT_BAR_Y);
        draw.drawString(font, CreateLang.translateDirect(requiredHeat.getTranslationKey()), 9, HEAT_BAR_Y + 6,
                requiredHeat.getColor(), false);

        // Arrow from the machine to the outputs
        if (!outputEntries.isEmpty()) {
            drawArrow(draw, MACHINE_RIGHT + 3, outputStartX() - 3, MACHINE_MIDDLE_Y - 5);
        }

        if (!noHeat) {
            MACHINE_DISPLAY.drawHeater(draw, MACHINE_X, HEATER_Y, MACHINE_SCALE, requiredHeat.visualizeAsBlazeBurner());
        }
        MACHINE_DISPLAY.drawMachine(draw, MACHINE_X, MACHINE_Y, MACHINE_SCALE);
    }

    /** Create's 42 px arrow, cut from the tail end when the gap is shorter. */
    private static void drawArrow(GuiGraphics draw, int startX, int endX, int y) {
        AllGuiTextures arrow = AllGuiTextures.JEI_ARROW;
        int length = endX - startX;
        if (length >= arrow.getWidth()) {
            arrow.render(draw, endX - arrow.getWidth(), y);
            return;
        }
        if (length <= 0) {
            return;
        }
        draw.blit(arrow.getLocation(), startX, y, arrow.getStartX() + arrow.getWidth() - length, arrow.getStartY(),
                length, arrow.getHeight());
    }

    private List<Component> machineTooltip() {
        // Step details live in the numeral tooltips; the machine tooltip keeps the setup and the failure rule
        return List.of(
                Component.translatable("guguaddons.emi.abyss_catalysis.machine"),
                Component.translatable("guguaddons.emi.abyss_catalysis.heat",
                        CreateLang.translateDirect(recipe.getRequiredHeat().getTranslationKey())),
                Component.translatable("guguaddons.emi.abyss_catalysis.failure").withStyle(ChatFormatting.RED)
        );
    }

    // ---------------------------------------------------------------- heat catalysts (Create's positions)

    private void addHeatCatalysts(WidgetHolder widgets) {
        HeatCondition requiredHeat = recipe.getRequiredHeat();
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.NONE)) {
            addHeatSlot(widgets, EmiStack.of(AllBlocks.BLAZE_BURNER.get()), 134);
        }
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.KINDLED)) {
            addHeatSlot(widgets, EmiStack.of(AllItems.BLAZE_CAKE.get()), 153);
        }
    }

    private static void addHeatSlot(WidgetHolder widgets, EmiStack stack, int x) {
        addSlotBackground(widgets, x, HEAT_BAR_Y + 1, AllGuiTextures.JEI_SLOT);
        widgets.addSlot(stack, x, HEAT_BAR_Y + 1).drawBack(false);
    }

    // ---------------------------------------------------------------- data

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

    private static String percent(float chance) {
        return chance == 0F ? "0%" : chance < 0.01F ? "<1%" : (int) (chance * 100F) + "%";
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
