package com.supermartijn642.pottery.content;

import com.supermartijn642.core.block.BaseBlockEntityType;
import com.supermartijn642.core.block.BlockShape;
import com.supermartijn642.core.item.BaseBlockItem;
import com.supermartijn642.core.item.ItemProperties;
import com.supermartijn642.core.registry.RegistrationHandler;
import com.supermartijn642.pottery.Pottery;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.EnumMap;
import java.util.Locale;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public enum PotType {

    DEFAULT("Decorated Pot", 0, BlockShape.createBlockShape(1, 0, 1, 15, 16, 15)),
    BASE("Pot", 0, BlockShape.or(
        BlockShape.createBlockShape(4, 0, 4, 12, 1, 12),
        BlockShape.createBlockShape(3, 1, 3, 13, 11, 13),
        BlockShape.createBlockShape(5, 11, 5, 11, 13, 11)
    )),
    SEALED("Sealed Pot", 1, BlockShape.or(
        BlockShape.createBlockShape(3, 0, 3, 13, 1, 13),
        BlockShape.createBlockShape(2, 1, 2, 14, 11, 14),
        BlockShape.createBlockShape(3, 11, 3, 13, 13, 13),
        BlockShape.createBlockShape(2, 13, 2, 14, 15, 14)
    )),
    WIDE("Wide Pot", 1, BlockShape.or(
        BlockShape.createBlockShape(2, 0, 2, 14, 8, 14),
        BlockShape.createBlockShape(5, 8, 5, 11, 10, 11),
        BlockShape.createBlockShape(4, 10, 4, 12, 11, 12)
    )),
    TALL("Tall Pot", 1, BlockShape.or(
        BlockShape.createBlockShape(5, 0, 5, 11, 1, 11),
        BlockShape.createBlockShape(4, 1, 4, 12, 15, 12),
        BlockShape.createBlockShape(6, 15, 6, 10, 17, 10),
        BlockShape.createBlockShape(5, 17, 5, 11, 19, 11)
    )),
    SMALL("Small Pot", 0, BlockShape.or(
        BlockShape.createBlockShape(4, 0, 4, 12, 9, 12),
        BlockShape.createBlockShape(6, 9, 6, 10, 11, 10)
    ));

    private final String identifier;
    private final String translation;
    private final int extraBrickCount;
    private final BlockShape shape;
    private final EnumMap<PotColor,PotBlock> blocks = new EnumMap<>(PotColor.class);
    private final EnumMap<PotColor,BaseBlockItem> items = new EnumMap<>(PotColor.class);
    private BaseBlockEntityType<PotBlockEntity> blockEntityType;

    PotType(String translation, int extraBrickCount, BlockShape shape){
        this.extraBrickCount = extraBrickCount;
        this.identifier = this.name().toLowerCase(Locale.ROOT) + "_pot";
        this.translation = translation;
        this.shape = shape;
    }

    public void registerBlocks(RegistrationHandler.Helper<Block> helper){
        for(PotColor color : PotColor.values()){
            if(this == DEFAULT && color == PotColor.BLANK)
                continue;
            this.blocks.put(color, helper.register(this.getIdentifier(color), new PotBlock(this, color)));
        }
    }

    public void registerBlockEntity(RegistrationHandler.Helper<BlockEntityType<?>> helper){
        this.blockEntityType = helper.register(this.identifier, BaseBlockEntityType.create((pos, state) -> new PotBlockEntity(this, pos, state), this.blocks.values().toArray(Block[]::new)));
    }

    public void registerItems(RegistrationHandler.Helper<Item> helper){
        for(PotColor color : PotColor.values()){
            if(this == DEFAULT && color == PotColor.BLANK)
                continue;
            PotBlock block = this.blocks.get(color);
            this.items.put(color, helper.register(this.getIdentifier(color), new BaseBlockItem(block, ItemProperties.create().group(Pottery.ITEM_GROUP))));
        }
    }

    public int getExtraBricks(){
        return this.extraBrickCount;
    }

    public BlockShape getShape(){
        return this.shape;
    }

    public String getIdentifier(){
        return this.identifier;
    }

    public String getIdentifier(PotColor color){
        return this.identifier + "_" + color.getIdentifier();
    }

    public Item getItem(PotColor color){
        if(this == DEFAULT && color == PotColor.BLANK)
            return Items.DECORATED_POT;
        return this.items.get(color);
    }

    public Block getBlock(PotColor color){
        if(this == DEFAULT && color == PotColor.BLANK)
            return Blocks.DECORATED_POT;
        return this.blocks.get(color);
    }

    public BaseBlockEntityType<PotBlockEntity> getBlockEntityType(){
        return this.blockEntityType;
    }

    public String getTranslation(){
        return this.translation;
    }
}
