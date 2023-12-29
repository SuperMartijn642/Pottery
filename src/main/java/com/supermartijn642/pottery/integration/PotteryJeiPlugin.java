package com.supermartijn642.pottery.integration;

import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.library.util.RecipeUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created 04/12/2023 by SuperMartijn642
 */
@JeiPlugin
public class PotteryJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid(){
        return new ResourceLocation(Pottery.MODID, "pot_recipes");
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration){
        registration.getCraftingCategory().addExtension(PotRecipe.class, new ICraftingCategoryExtension<PotRecipe>() {
            @Override
            public void setRecipe(RecipeHolder<PotRecipe> recipeHolder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses){
                PotRecipe recipe = recipeHolder.value();
                // Inputs
                int width = this.getWidth(recipeHolder);
                int height = this.getHeight(recipeHolder);
                List<List<ItemStack>> inputs = recipe.getIngredients().stream()
                    .map(Ingredient::getItems)
                    .map(Arrays::asList)
                    .collect(Collectors.toCollection(ArrayList::new));
                Ingredient dyeIngredient = recipe.getDyeIngredient();
                if(dyeIngredient != null){
                    for(int i = 0; i < inputs.size(); i++){
                        if(inputs.get(i).isEmpty()){
                            inputs.set(i, Arrays.asList(dyeIngredient.getItems()));
                            break;
                        }
                    }
                }
                craftingGridHelper.createAndSetInputs(builder, inputs, width, height);
                // Output
                ItemStack resultItem = RecipeUtil.getResultItem(recipe);
                craftingGridHelper.createAndSetOutputs(builder, List.of(resultItem));
            }

            @Override
            public Optional<ResourceLocation> getRegistryName(RecipeHolder<PotRecipe> recipeHolder){
                return Optional.of(recipeHolder.id());
            }

            @Override
            public int getWidth(RecipeHolder<PotRecipe> recipeHolder){
                return recipeHolder.value().getWidth();
            }

            @Override
            public int getHeight(RecipeHolder<PotRecipe> recipeHolder){
                return recipeHolder.value().getHeight();
            }
        });
    }
}
