package com.gugucraft.guguaddons.compat.emi;

import com.gugucraft.guguaddons.config.Config;
import com.gugucraft.guguaddons.recipe.CentrifugationRecipe;
import com.gugucraft.guguaddons.recipe.PressurizingRecipe;
import com.gugucraft.guguaddons.recipe.VacuumizingRecipe;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.gugucraft.guguaddons.registry.ModItems;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        registry.removeRecipes(recipe -> MachineRecipeStageManager.clientShouldHide(recipe.getBackingRecipe())
                || recipe.getId() != null && MachineRecipeStageManager.clientShouldHide(recipe.getId()));

        registerSlashBackSmithing(registry);
        registerVacuumizing(registry);
        registerPressurizing(registry);
        registerCentrifugation(registry);
    }

    private void registerSlashBackSmithing(EmiRegistry registry) {
        // Create Base Item (Broken Terminal)
        ItemStack damagedStack = new ItemStack(ModItems.DEATH_RECALL_ITEM.get());
        damagedStack.setDamageValue(damagedStack.getMaxDamage() - 1);
        EmiIngredient base = EmiStack.of(damagedStack);

        // Create Result Item (Repaired Terminal)
        ItemStack repairedStack = new ItemStack(ModItems.DEATH_RECALL_ITEM.get());
        repairedStack.setDamageValue(0);
        EmiStack result = EmiStack.of(repairedStack);

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
                return List.of(currentTemplate(), base, currentMaterial());
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
                EmiIngredient template = currentTemplate();
                EmiIngredient material = currentMaterial();

                widgets.addSlot(template, 0, 0);
                widgets.addSlot(base, 18, 0);
                widgets.addSlot(material, 36, 0);

                widgets.addTexture(EmiTexture.EMPTY_ARROW, 62, 1);

                widgets.addSlot(result, 94, 0).recipeContext(this);
            }

            private EmiIngredient currentTemplate() {
                return EmiStack.of(Config.getEffectiveSlashBackRefillTemplateItem());
            }

            private EmiIngredient currentMaterial() {
                return EmiStack.of(Config.getEffectiveSlashBackRefillMaterialItem());
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
                .stream()
                .filter(holder -> MachineRecipeStageManager.clientCanSee(recipeType, holder.id()))
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
                .stream()
                .filter(holder -> MachineRecipeStageManager.clientCanSee(recipeType, holder.id()))
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
                .stream()
                .filter(holder -> MachineRecipeStageManager.clientCanSee(recipeType, holder.id()))
                .forEach(holder -> registry.addRecipe(
                        new CentrifugationEmiRecipe(CENTRIFUGATION_CATEGORY, holder.id(), holder.value())));
    }
}
