package com.supermartijn642.pottery.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.PotDecorations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created 01/12/2023 by SuperMartijn642
 */
public class PotRecipe implements CraftingRecipe {

    public static final RecipeSerializer<PotRecipe> SERIALIZER = new RecipeSerializer<>(Serializer.CODEC, Serializer.STREAM_CODEC);

    private final ShapedRecipe recipe;
    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate output;
    private final Ingredient dyeIngredient;
    private final int[] sherdIndices;

    public PotRecipe(ShapedRecipe recipe, Ingredient dyeIngredient, int[] sherdIndices){
        this.recipe = recipe;
        this.pattern = recipe.pattern;
        this.output = recipe.result;
        this.dyeIngredient = dyeIngredient;
        this.sherdIndices = sherdIndices;
    }

    public int getWidth(){
        return this.recipe.getWidth();
    }

    public int getHeight(){
        return this.recipe.getHeight();
    }

    @Override
    public boolean matches(CraftingInput input, Level level){
        return this.findRecipeDecorations(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input){
        ItemStack stack = this.recipe.assemble(input);

        // Add the decorations
        PotDecorations decorations = this.findRecipeDecorations(input);
        Objects.requireNonNull(decorations);
        if(!decorations.equals(PotDecorations.EMPTY))
            stack.set(DataComponents.POT_DECORATIONS, decorations);

        return stack;
    }

    @Override
    public boolean showNotification(){
        return this.recipe.showNotification();
    }

    @Override
    public String group(){
        return this.recipe.group();
    }

    @Override
    public List<RecipeDisplay> display(){
        List<SlotDisplay> inputs = this.pattern.ingredients().stream().map(o -> o.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).collect(Collectors.toCollection(ArrayList::new));
        if(this.dyeIngredient != null){
            for(int i = 0; i < inputs.size(); i++){
                if(inputs.get(i) == SlotDisplay.Empty.INSTANCE){
                    inputs.set(i, this.dyeIngredient.display());
                    break;
                }
            }
        }
        return List.of(new ShapedCraftingRecipeDisplay(
            this.pattern.width(),
            this.pattern.height(),
            inputs,
            new SlotDisplay.ItemStackSlotDisplay(this.output),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }

    private PotDecorations findRecipeDecorations(CraftingInput input){
        if(this.dyeIngredient == null ? input.ingredientCount() != this.pattern.ingredientCount
            : input.ingredientCount() < this.pattern.ingredientCount || input.ingredientCount() > this.pattern.ingredientCount + 1)
            return null;

        for(int x = 0; x <= input.width() - this.getWidth(); ++x){
            for(int y = 0; y <= input.height() - this.getHeight(); ++y){
                PotDecorations decorations = this.matchesSubGrid(input, x, y, true);
                if(decorations == null)
                    decorations = this.matchesSubGrid(input, x, y, false);
                if(decorations != null)
                    return decorations;
            }
        }
        return null;
    }

    private PotDecorations matchesSubGrid(CraftingInput input, int startX, int startY, boolean mirrored){
        boolean foundDye = false;
        for(int x = 0; x < input.width(); ++x){
            for(int y = 0; y < input.height(); ++y){
                ItemStack stack = input.getItem(x + y * input.width());
                if(this.dyeIngredient != null && this.dyeIngredient.test(stack)){
                    if(foundDye)
                        return null;
                    foundDye = true;
                    continue;
                }

                int relativeX = x - startX;
                int relativeY = y - startY;
                if(relativeX >= 0 && relativeY >= 0 && relativeX < this.getWidth() && relativeY < this.getHeight()){
                    Optional<Ingredient> ingredient = this.recipe.getIngredients().get(mirrored ?
                        this.getWidth() - relativeX - 1 + relativeY * this.getWidth() :
                        relativeX + relativeY * this.getWidth()
                    );
                    if(!Ingredient.testOptionalIngredient(ingredient, stack))
                        return null;
                }else if(!stack.isEmpty())
                    return null;
            }
        }

        if(this.dyeIngredient != null && !foundDye)
            return null;

        Item front = input.getItem(startX + this.sherdIndices[0] % this.getWidth() + (startY + this.sherdIndices[0] / this.getWidth()) * input.width()).getItem();
        Item left = input.getItem(startX + this.sherdIndices[1] % this.getWidth() + (startY + this.sherdIndices[1] / this.getWidth()) * input.width()).getItem();
        Item right = input.getItem(startX + this.sherdIndices[2] % this.getWidth() + (startY + this.sherdIndices[2] / this.getWidth()) * input.width()).getItem();
        Item back = input.getItem(startX + this.sherdIndices[3] % this.getWidth() + (startY + this.sherdIndices[3] / this.getWidth()) * input.width()).getItem();
        return new PotDecorations(back, left, right, front);
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo(){
        return this.recipe.placementInfo();
    }

    @Override
    public CraftingBookCategory category(){
        return this.recipe.category();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input){
        return this.recipe.getRemainingItems(input);
    }

    @Override
    public RecipeBookCategory recipeBookCategory(){
        return this.recipe.recipeBookCategory();
    }

    private static class Serializer {

        private static final Function<Integer,DataResult<Integer>> GEQUAL_TO_ZERO = integer -> integer < 0 ? DataResult.error(() -> "Value '" + integer + "' is less than 0!") : DataResult.success(integer);
        private static final MapCodec<PotRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ShapedRecipe.MAP_CODEC.fieldOf("recipe").forGetter(recipe -> null),
            Ingredient.CODEC.optionalFieldOf("dye_ingredient").forGetter(recipe -> Optional.of(recipe.dyeIngredient)),
            Codec.INT.flatXmap(GEQUAL_TO_ZERO, GEQUAL_TO_ZERO).listOf().fieldOf("sherds").forGetter(recipe -> IntStream.of(recipe.sherdIndices).boxed().toList())
        ).apply(instance, (shapedRecipe, dyeIngredient, sherdIndices) -> new PotRecipe(
            shapedRecipe,
            dyeIngredient.orElse(null),
            sherdIndices.stream().mapToInt(i -> i).toArray()
        )));
        private static final StreamCodec<RegistryFriendlyByteBuf,PotRecipe> STREAM_CODEC = StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        public static PotRecipe fromNetwork(RegistryFriendlyByteBuf buffer){
            ShapedRecipe shapedRecipe = ShapedRecipe.STREAM_CODEC.decode(buffer);
            Ingredient dyeIngredient = buffer.readBoolean() ? Ingredient.CONTENTS_STREAM_CODEC.decode(buffer) : null;
            int[] sherdIndices = buffer.readVarIntArray(4);
            if(sherdIndices.length != 4)
                throw new IllegalArgumentException();
            return new PotRecipe(
                shapedRecipe,
                dyeIngredient,
                sherdIndices
            );
        }

        public static void toNetwork(RegistryFriendlyByteBuf buffer, PotRecipe recipe){
            ShapedRecipe.STREAM_CODEC.encode(buffer, recipe.recipe);
            buffer.writeBoolean(recipe.dyeIngredient != null);
            if(recipe.dyeIngredient != null)
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.dyeIngredient);
            buffer.writeVarIntArray(recipe.sherdIndices);
        }
    }
}
