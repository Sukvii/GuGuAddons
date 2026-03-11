package com.gugucraft.guguaddons.compat.emi;

import com.gugucraft.guguaddons.Config;
import com.gugucraft.guguaddons.recipe.CentrifugationRecipe;
import com.gugucraft.guguaddons.recipe.PressurizingRecipe;
import com.gugucraft.guguaddons.recipe.VacuumizingRecipe;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.gugucraft.guguaddons.registry.ModItems;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.simibubi.create.AllBlocks;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

@EmiEntrypoint
public class ModEmiPlugin implements EmiPlugin {
    private static final EmiRecipeCategory VACUUMIZING_CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("guguaddons", "vacuumizing"),
            EmiStack.of(ModBlocks.VACUUM_CHAMBER.get())) {
        @Override
        public Component getName() {
            return Component.translatable("tooltip.guguaddons.vacuum_chamber.mode.vacuumizing");
        }
    };

    private static final EmiRecipeCategory PRESSURIZING_CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("guguaddons", "pressurizing"),
            EmiStack.of(ModBlocks.VACUUM_CHAMBER.get())) {
        @Override
        public Component getName() {
            return Component.translatable("tooltip.guguaddons.vacuum_chamber.mode.pressurizing");
        }
    };

    private static final EmiRecipeCategory CENTRIFUGATION_CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("guguaddons", "centrifugation"),
            EmiStack.of(ModBlocks.CENTRIFUGE.get())) {
        @Override
        public Component getName() {
            return Component.translatable("block.guguaddons.centrifuge");
        }
    };

    @Override
    public void register(EmiRegistry registry) {
        registerSlashBackSmithing(registry);
        registerVacuumizing(registry);
        registerPressurizing(registry);
        registerCentrifugation(registry);
    }

    private void registerSlashBackSmithing(EmiRegistry registry) {
        // 1. Get Configured Template
        String templateId = Config.SLASH_BACK_REFILL_TEMPLATE.get();
        Item templateItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(templateId));
        EmiIngredient template = EmiStack.of(templateItem);

        // 2. Get Configured Material
        String materialId = Config.SLASH_BACK_REFILL_MATERIAL.get();
        Item materialItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(materialId));
        EmiIngredient material = EmiStack.of(materialItem);

        // 3. Create Base Item (Broken Terminal)
        ItemStack damagedStack = new ItemStack(ModItems.DEATH_RECALL_ITEM.get());
        damagedStack.setDamageValue(damagedStack.getMaxDamage() - 1);
        EmiIngredient base = EmiStack.of(damagedStack);

        // 4. Create Result Item (Repaired Terminal)
        ItemStack repairedStack = new ItemStack(ModItems.DEATH_RECALL_ITEM.get());
        repairedStack.setDamageValue(0);
        EmiStack result = EmiStack.of(repairedStack);

        // 5. Create EmiSmithingRecipe
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("guguaddons", "/slash_back_smithing_emi");

        registry.addRecipe(new EmiRecipe() {
            @Override
            public EmiRecipeCategory getCategory() {
                return VanillaEmiRecipeCategories.SMITHING;
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public List<EmiIngredient> getInputs() {
                return List.of(template, base, material);
            }

            @Override
            public List<EmiStack> getOutputs() {
                return List.of(result);
            }

            @Override
            public int getDisplayWidth() {
                return 112;
            }

            @Override
            public int getDisplayHeight() {
                return 18;
            }

            @Override
            public void addWidgets(WidgetHolder widgets) {
                // Mimic standard smithing layout
                // Template (0,0), Base (18,0), Material (36,0) -> Arrow -> Output (94,0)
                widgets.addSlot(template, 0, 0);
                widgets.addSlot(base, 18, 0);
                widgets.addSlot(material, 36, 0);

                // EMI API usually has standard texture constants.
                // EmiTexture.EMPTY_ARROW is at 62, 1 in standard smithing.
                widgets.addTexture(EmiTexture.EMPTY_ARROW, 62, 1);

                widgets.addSlot(result, 94, 0).recipeContext(this);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void registerVacuumizing(EmiRegistry registry) {
        registry.addCategory(VACUUMIZING_CATEGORY);
        registry.addWorkstation(VACUUMIZING_CATEGORY, EmiStack.of(ModBlocks.VACUUM_CHAMBER.get()));
        registry.addWorkstation(VACUUMIZING_CATEGORY, EmiStack.of(AllBlocks.BASIN.get()));

        RecipeType<VacuumizingRecipe> recipeType =
                (RecipeType<VacuumizingRecipe>) (RecipeType<?>) ModRecipes.VACUUMIZING.getType();
        registry.getRecipeManager()
                .getAllRecipesFor(recipeType)
                .forEach(holder -> registry.addRecipe(
                        new CompressorEmiRecipe(VACUUMIZING_CATEGORY, holder.id(), holder.value(), false)));
    }

    @SuppressWarnings("unchecked")
    private void registerPressurizing(EmiRegistry registry) {
        registry.addCategory(PRESSURIZING_CATEGORY);
        registry.addWorkstation(PRESSURIZING_CATEGORY, EmiStack.of(ModBlocks.VACUUM_CHAMBER.get()));
        registry.addWorkstation(PRESSURIZING_CATEGORY, EmiStack.of(AllBlocks.BASIN.get()));

        RecipeType<PressurizingRecipe> recipeType =
                (RecipeType<PressurizingRecipe>) (RecipeType<?>) ModRecipes.PRESSURIZING.getType();
        registry.getRecipeManager()
                .getAllRecipesFor(recipeType)
                .forEach(holder -> registry.addRecipe(
                        new CompressorEmiRecipe(PRESSURIZING_CATEGORY, holder.id(), holder.value(), true)));
    }

    @SuppressWarnings("unchecked")
    private void registerCentrifugation(EmiRegistry registry) {
        registry.addCategory(CENTRIFUGATION_CATEGORY);
        registry.addWorkstation(CENTRIFUGATION_CATEGORY, EmiStack.of(ModBlocks.CENTRIFUGE.get()));

        RecipeType<CentrifugationRecipe> recipeType =
                (RecipeType<CentrifugationRecipe>) (RecipeType<?>) ModRecipes.CENTRIFUGATION.getType();
        registry.getRecipeManager()
                .getAllRecipesFor(recipeType)
                .forEach(holder -> registry.addRecipe(
                        new CentrifugationEmiRecipe(CENTRIFUGATION_CATEGORY, holder.id(), holder.value())));
    }
}
