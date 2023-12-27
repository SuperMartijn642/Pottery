package com.supermartijn642.pottery.content;

import com.supermartijn642.pottery.Pottery;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public enum PotColor {
    BLANK(null, 0, 100, 0, null),
    WHITE("White", 0, 0, 25, () -> Ingredient.of(Items.WHITE_DYE)),
    ORANGE("Orange", 0, 130, 0, () -> Ingredient.of(Items.ORANGE_DYE)),
    MAGENTA("Magenta", -70, 115, 0, () -> Ingredient.of(Items.MAGENTA_DYE)),
    LIGHT_BLUE("Light Blue", -170, 110, 0, () -> Ingredient.of(Items.LIGHT_BLUE_DYE)),
    YELLOW("Yellow", 35, 120, 0, () -> Ingredient.of(Items.YELLOW_DYE)),
    LIME("Lime", 75, 125, 0, () -> Ingredient.of(Items.LIME_DYE)),
    PINK("Pink", -35, 115, 5, () -> Ingredient.of(Items.PINK_DYE)),
    GRAY("Gray", 0, 0, -25, () -> Ingredient.of(Items.GRAY_DYE)),
    LIGHT_GRAY("Light Gray", 0, 0, 0, () -> Ingredient.of(Items.LIGHT_GRAY_DYE)),
    CYAN("Cyan", 155, 100, 0, () -> Ingredient.of(Items.CYAN_DYE)),
    PURPLE("Purple", -95, 120, 0, () -> Ingredient.of(Items.PURPLE_DYE)),
    BLUE("Blue", -145, 115, -5, () -> Ingredient.of(Items.BLUE_DYE)),
    BROWN("Brown", 0, 125, -30, () -> Ingredient.of(Items.BROWN_DYE)),
    GREEN("Green", 95, 120, -20, () -> Ingredient.of(Items.GREEN_DYE)),
    RED("Red", -10, 140, -20, () -> Ingredient.of(Items.RED_DYE)),
    BLACK("Black", 0, 0, -60, () -> Ingredient.of(Items.BLACK_DYE));

    private final String identifier;
    private final String translation;
    private final int hueShift, saturationShift, brightnessShift;
    private final Supplier<Ingredient> dyeIngredient;

    PotColor(String translation, int hueShift, int saturationShift, int brightnessShift, Supplier<Ingredient> dyeIngredient){
        this.dyeIngredient = dyeIngredient;
        this.identifier = this.name().toLowerCase(Locale.ROOT);
        this.translation = translation;
        this.hueShift = hueShift;
        this.saturationShift = saturationShift;
        this.brightnessShift = brightnessShift;
    }

    public String getIdentifier(){
        return this.identifier;
    }

    public String getTranslation(){
        return this.translation;
    }

    public int getHueShift(){
        return this.hueShift;
    }

    public int getSaturationShift(){
        return this.saturationShift;
    }

    public int getBrightnessShift(){
        return this.brightnessShift;
    }

    public Ingredient getDyeIngredient(){
        return this.dyeIngredient.get();
    }

    public ResourceLocation getPatternLocation(ResourceKey<String> key){
        ResourceLocation texture = DecoratedPotPatterns.location(key);
        if(this == BLANK)
            return texture;

        if(key.location().getNamespace().equals("minecraft"))
            return new ResourceLocation(Pottery.MODID, "patterns/" + this.getIdentifier() + "/" + texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1));

        return texture;
    }

    public static DyeColor dyeForColor(PotColor color){
        return switch(color){
            case WHITE -> DyeColor.WHITE;
            case ORANGE -> DyeColor.ORANGE;
            case MAGENTA -> DyeColor.MAGENTA;
            case LIGHT_BLUE -> DyeColor.LIGHT_BLUE;
            case YELLOW -> DyeColor.YELLOW;
            case LIME -> DyeColor.LIME;
            case PINK -> DyeColor.PINK;
            case GRAY -> DyeColor.GRAY;
            case LIGHT_GRAY -> DyeColor.LIGHT_GRAY;
            case CYAN -> DyeColor.CYAN;
            case PURPLE -> DyeColor.PURPLE;
            case BLUE -> DyeColor.BLUE;
            case BROWN -> DyeColor.BROWN;
            case GREEN -> DyeColor.GREEN;
            case RED -> DyeColor.RED;
            case BLACK -> DyeColor.BLACK;
            default -> null;
        };
    }

    public static PotColor colorForDye(DyeColor dye){
        return switch(dye){
            case WHITE -> WHITE;
            case ORANGE -> ORANGE;
            case MAGENTA -> MAGENTA;
            case LIGHT_BLUE -> LIGHT_BLUE;
            case YELLOW -> YELLOW;
            case LIME -> LIME;
            case PINK -> PINK;
            case GRAY -> GRAY;
            case LIGHT_GRAY -> LIGHT_GRAY;
            case CYAN -> CYAN;
            case PURPLE -> PURPLE;
            case BLUE -> BLUE;
            case BROWN -> BROWN;
            case GREEN -> GREEN;
            case RED -> RED;
            case BLACK -> BLACK;
            //noinspection UnnecessaryDefault other mods might add new entries to the DyeColor enum
            default -> null;
        };
    }
}
