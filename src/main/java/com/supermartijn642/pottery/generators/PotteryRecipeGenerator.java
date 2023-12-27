package com.supermartijn642.pottery.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.core.generator.ResourceGenerator;
import com.supermartijn642.core.generator.ResourceType;
import com.supermartijn642.core.registry.Registries;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created 01/12/2023 by SuperMartijn642
 */
public class PotteryRecipeGenerator extends ResourceGenerator {

    private final Map<ResourceLocation,RecipeBuilder> recipes = new LinkedHashMap<>();

    public PotteryRecipeGenerator(ResourceCache cache){
        super(Pottery.MODID, cache);
    }

    @Override
    public String getName(){
        return this.modName + " Recipe Generator";
    }

    public RecipeBuilder recipe(String location){
        this.cache.trackToBeGeneratedResource(ResourceType.DATA, this.modid, "recipes", location, ".json");
        return this.recipes.computeIfAbsent(new ResourceLocation(this.modid, location), i -> new RecipeBuilder());
    }

    @Override
    public void generate(){
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                RecipeBuilder recipe = this.recipe(type.getIdentifier(color));
                switch(type){
                    case DEFAULT -> recipe.pattern(
                        " # ",
                        "# #",
                        " # "
                    ).sherds(7, 3, 5, 1);
                    case BASE -> recipe.pattern(
                        "###",
                        " # "
                    ).sherds(4, 0, 2, 1);
                    case SEALED -> recipe.pattern(
                            " P ",
                            "# #",
                            "#B#"
                        ).sherds(8, 6, 5, 3)
                        .input('P', Ingredient.of(Items.STONE_PRESSURE_PLATE, Items.POLISHED_BLACKSTONE_PRESSURE_PLATE))
                        .input('B', Items.BRICK);
                    case WIDE -> recipe.pattern(
                            "# #",
                            "#B#"
                        ).sherds(5, 3, 2, 0)
                        .input('B', Items.BRICK);
                    case TALL -> recipe.pattern(
                            "# #",
                            "# #",
                            " B "
                        ).sherds(5, 3, 2, 0)
                        .input('B', Items.BRICK);
                    case SMALL -> recipe.pattern(
                        " # ",
                        "###"
                    ).sherds(4, 3, 5, 1);
                }
                if(color != PotColor.BLANK)
                    recipe.dye(color.getDyeIngredient());
                recipe.input('#', Ingredient.of(ItemTags.DECORATED_POT_INGREDIENTS))
                    .output(type.getItem(color));
            }
        }
    }

    @Override
    public void save(){
        for(Map.Entry<ResourceLocation,RecipeBuilder> entry : this.recipes.entrySet()){
            ResourceLocation location = entry.getKey();
            RecipeBuilder recipe = entry.getValue();

            // Convert the recipe to json
            JsonObject json = new JsonObject();
            json.addProperty("type", "pottery:pot");
            JsonArray sherds = new JsonArray(4);
            sherds.add(recipe.sherdIndices[0]);
            sherds.add(recipe.sherdIndices[1]);
            sherds.add(recipe.sherdIndices[2]);
            sherds.add(recipe.sherdIndices[3]);
            json.add("sherds", sherds);
            if(recipe.dyeIngredient != null)
                json.add("dye_ingredient", Ingredient.CODEC_NONEMPTY.encodeStart(JsonOps.INSTANCE, recipe.dyeIngredient).getOrThrow(false, s -> {}));
            JsonObject recipeJson = new JsonObject();
            recipeJson.addProperty("show_notification", true);
            JsonArray pattern = new JsonArray();
            Arrays.stream(recipe.pattern).forEach(pattern::add);
            recipeJson.add("pattern", pattern);
            JsonObject keys = new JsonObject();
            recipe.inputs.forEach((key, ingredient) -> keys.add(key.toString(), Ingredient.CODEC_NONEMPTY.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow(false, s -> {})));
            recipeJson.add("key", keys);
            JsonObject result = new JsonObject();
            result.addProperty("item", Registries.ITEMS.getIdentifier(recipe.output).toString());
            recipeJson.add("result", result);
            json.add("recipe", recipeJson);

            // Save the recipe
            this.cache.saveJsonResource(ResourceType.DATA, json, location.getNamespace(), "recipes", location.getPath());
        }
    }

    private static class RecipeBuilder {
        private String[] pattern;
        private int[] sherdIndices;
        private final Map<Character,Ingredient> inputs = new LinkedHashMap<>();
        private Ingredient dyeIngredient;
        private Item output;

        public RecipeBuilder pattern(String... pattern){
            if(pattern.length == 0 || pattern.length > 3)
                throw new IllegalArgumentException("Pattern must consist of 1 to 3 lines!");
            int width = pattern[0].length();
            for(String s : pattern){
                if(s.length() != width)
                    throw new IllegalArgumentException("All lines in the pattern must have the same length!");
                if(s.length() == 0 || s.length() > 3)
                    throw new IllegalArgumentException("Pattern must have a width of 1 to 3 characters!");
            }
            this.pattern = pattern;
            return this;
        }

        public RecipeBuilder sherds(int front, int left, int right, int back){
            this.sherdIndices = new int[]{front, left, right, back};
            return this;
        }

        public RecipeBuilder input(char key, Ingredient input){
            if(this.inputs.put(key, input) != null)
                throw new IllegalArgumentException("Duplicate input for character '" + key + "'!");
            return this;
        }

        public RecipeBuilder input(char key, Item item){
            return this.input(key, Ingredient.of(item));
        }

        public RecipeBuilder dye(Ingredient ingredient){
            this.dyeIngredient = ingredient;
            return this;
        }

        public RecipeBuilder dye(Item dye){
            this.dye(Ingredient.of(dye));
            return this;
        }

        public RecipeBuilder output(Item output){
            this.output = output;
            return this;
        }
    }
}
