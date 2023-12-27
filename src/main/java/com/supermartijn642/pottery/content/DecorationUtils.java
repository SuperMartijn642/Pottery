package com.supermartijn642.pottery.content;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;

/**
 * Created 01/12/2023 by SuperMartijn642
 */
public class DecorationUtils {

    public static Item getDecorationItem(DecoratedPotBlockEntity.Decorations decorations, Direction potFacing, Direction side){
        return switch((side.get2DDataValue() - potFacing.get2DDataValue() + 4) % 4){
            case 0 -> decorations.back();
            case 1 -> decorations.right();
            case 2 -> decorations.front();
            case 3 -> decorations.left();
            default ->
                throw new IllegalStateException("Unexpected value: " + (side.get2DDataValue() - potFacing.get2DDataValue() + 4) % 4);
        };
    }

    public static DecoratedPotBlockEntity.Decorations setDecorationItem(DecoratedPotBlockEntity.Decorations decorations, Direction potFacing, Direction side, Item item){
        return switch((side.get2DDataValue() - potFacing.get2DDataValue() + 4) % 4){
            case 0 -> new DecoratedPotBlockEntity.Decorations(item, decorations.left(), decorations.right(), decorations.front());
            case 1 -> new DecoratedPotBlockEntity.Decorations(decorations.back(), decorations.left(), item, decorations.front());
            case 2 -> new DecoratedPotBlockEntity.Decorations(decorations.back(), decorations.left(), decorations.right(), item);
            case 3 -> new DecoratedPotBlockEntity.Decorations(decorations.back(), item, decorations.right(), decorations.front());
            default ->
                throw new IllegalStateException("Unexpected value: " + (side.get2DDataValue() - potFacing.get2DDataValue() + 4) % 4);
        };
    }
}
