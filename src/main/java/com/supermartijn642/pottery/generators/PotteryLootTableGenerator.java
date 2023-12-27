package com.supermartijn642.pottery.generators;

import com.supermartijn642.core.generator.LootTableGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotType;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.storage.loot.entries.DynamicLoot;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;

/**
 * Created 30/11/2023 by SuperMartijn642
 */
public class PotteryLootTableGenerator extends LootTableGenerator {

    public PotteryLootTableGenerator(ResourceCache cache){
        super(Pottery.MODID, cache);
    }

    @Override
    public void generate(){
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                if(type == PotType.DEFAULT && color == PotColor.BLANK)
                    continue;

                Block block = type.getBlock(color);
                this.lootTable(block)
                    .blockParameters()
                    .pool(pool ->
                        pool.entry(
                            DynamicLoot.dynamicEntry(DecoratedPotBlock.SHERDS_DYNAMIC_DROP_ID)
                                .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(ItemTags.BREAKS_DECORATED_POTS)))
                                .when(BlockLootSubProvider.HAS_NO_SILK_TOUCH)
                                .otherwise(
                                    LootItem.lootTableItem(block)
                                        .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY).copy("sherds", "BlockEntityTag.sherds"))
                                ).build()
                        )
                    );
            }
        }
    }
}
