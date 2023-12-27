package com.supermartijn642.pottery;

import com.supermartijn642.core.item.CreativeItemGroup;
import com.supermartijn642.core.registry.GeneratorRegistrationHandler;
import com.supermartijn642.core.registry.RegistrationHandler;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotEventHandlers;
import com.supermartijn642.pottery.content.PotRecipe;
import com.supermartijn642.pottery.content.PotType;
import com.supermartijn642.pottery.generators.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.Items;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class Pottery implements ModInitializer {

    public static final String MODID = "pottery";

    public static final CreativeItemGroup ITEM_GROUP = CreativeItemGroup.create(MODID, () -> PotType.DEFAULT.getItem(PotColor.WHITE))
        .filler(consumer -> {
            for(PotType type : PotType.values()){
                for(PotColor color : PotColor.values()){
                    consumer.accept(type.getItem(color).getDefaultInstance());
                }
            }
        });

    @Override
    public void onInitialize(){
        PotEventHandlers.registerListeners();

        PotteryConfig.init();
        register();
        registerGenerators();
    }

    private static void register(){
        RegistrationHandler handler = RegistrationHandler.get(MODID);
        for(PotType type : PotType.values()){
            handler.registerBlockCallback(type::registerBlocks);
            handler.registerBlockEntityTypeCallback(type::registerBlockEntity);
            handler.registerItemCallback(type::registerItems);
        }
        handler.registerRecipeSerializer("pot", () -> PotRecipe.SERIALIZER);
    }

    public static void registerGenerators(){
        GeneratorRegistrationHandler handler = GeneratorRegistrationHandler.get(MODID);
        handler.addGenerator(PotteryAtlasSourceGenerator::new);
        handler.addGenerator(PotteryBlockStateGenerator::new);
        handler.addGenerator(PotteryLanguageGenerator::new);
        handler.addGenerator(PotteryLootTableGenerator::new);
        handler.addGenerator(PotteryModelGenerator::new);
        handler.addGenerator(PotteryTextureGenerator::new);
        handler.addGenerator(PotteryRecipeGenerator::new);
    }
}
