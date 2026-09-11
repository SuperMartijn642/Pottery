package com.supermartijn642.pottery.content;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.PotDecorations;

import java.util.Optional;

/**
 * Created 01/12/2023 by SuperMartijn642
 */
public class DecorationUtils {

    public static Optional<ItemStackTemplate> getDecorationItem(PotDecorations decorations, Direction potFacing, Direction side){
        return switch((side.get2DDataValue() - potFacing.get2DDataValue() + 4) % 4){
            case 0 -> decorations.back();
            case 1 -> decorations.right();
            case 2 -> decorations.front();
            case 3 -> decorations.left();
            default -> throw new IllegalStateException("Unexpected value: " + (side.get2DDataValue() - potFacing.get2DDataValue() + 4) % 4);
        };
    }

    public static PotDecorations setDecorationItem(PotDecorations decorations, Direction potFacing, Direction side, Optional<ItemStackTemplate> item){
        return switch((side.get2DDataValue() - potFacing.get2DDataValue() + 4) % 4){
            case 0 -> new PotDecorations(item, decorations.left(), decorations.right(), decorations.front());
            case 1 -> new PotDecorations(decorations.back(), decorations.left(), item, decorations.front());
            case 2 -> new PotDecorations(decorations.back(), decorations.left(), decorations.right(), item);
            case 3 -> new PotDecorations(decorations.back(), item, decorations.right(), decorations.front());
            default -> throw new IllegalStateException("Unexpected value: " + (side.get2DDataValue() - potFacing.get2DDataValue() + 4) % 4);
        };
    }
}
