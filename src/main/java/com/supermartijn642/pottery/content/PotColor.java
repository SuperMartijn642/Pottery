package com.supermartijn642.pottery.content;

import com.supermartijn642.pottery.Pottery;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.neoforged.neoforge.common.Tags;

import java.util.Locale;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public enum PotColor {
    BLANK(null, 0, 100, 0, null),
    WHITE("White", 0, 0, 25, Tags.Items.DYES_WHITE),
    ORANGE("Orange", 0, 130, 0, Tags.Items.DYES_ORANGE),
    MAGENTA("Magenta", -70, 115, 0, Tags.Items.DYES_MAGENTA),
    LIGHT_BLUE("Light Blue", -170, 110, 0, Tags.Items.DYES_LIGHT_BLUE),
    YELLOW("Yellow", 35, 120, 0, Tags.Items.DYES_YELLOW),
    LIME("Lime", 75, 125, 0, Tags.Items.DYES_LIME),
    PINK("Pink", -35, 115, 5, Tags.Items.DYES_PINK),
    GRAY("Gray", 0, 0, -25, Tags.Items.DYES_GRAY),
    LIGHT_GRAY("Light Gray", 0, 0, 0, Tags.Items.DYES_LIGHT_GRAY),
    CYAN("Cyan", 155, 100, 0, Tags.Items.DYES_CYAN),
    PURPLE("Purple", -95, 120, 0, Tags.Items.DYES_PURPLE),
    BLUE("Blue", -145, 115, -5, Tags.Items.DYES_BLUE),
    BROWN("Brown", 0, 125, -30, Tags.Items.DYES_BROWN),
    GREEN("Green", 95, 120, -20, Tags.Items.DYES_GREEN),
    RED("Red", -10, 140, -20, Tags.Items.DYES_RED),
    BLACK("Black", 0, 0, -60, Tags.Items.DYES_BLACK);

    private final String identifier;
    private final String translation;
    private final int hueShift, saturationShift, brightnessShift;
    private final TagKey<Item> dyeIngredient;

    PotColor(String translation, int hueShift, int saturationShift, int brightnessShift, TagKey<Item> dyeIngredient){
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

    public TagKey<Item> getDyeIngredient(){
        return this.dyeIngredient;
    }

    public Identifier getPatternLocation(ResourceKey<DecoratedPotPattern> key){
        DecoratedPotPattern pattern = BuiltInRegistries.DECORATED_POT_PATTERN.getOrThrow(key).value();
        Identifier texture = Sheets.DECORATED_POT_MAPPER.apply(pattern.assetId()).texture();
        if(this == BLANK)
            return texture;

        if(key.identifier().getNamespace().equals("minecraft"))
            return Identifier.fromNamespaceAndPath(Pottery.MODID, "patterns/" + this.getIdentifier() + "/" + texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1));

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
