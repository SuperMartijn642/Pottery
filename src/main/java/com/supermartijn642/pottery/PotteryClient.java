package com.supermartijn642.pottery;

import com.supermartijn642.core.registry.ClientRegistrationHandler;
import com.supermartijn642.core.render.TextureAtlases;
import com.supermartijn642.pottery.content.PotBakedModel;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotType;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotteryClient implements ClientModInitializer {

    @Override
    public void onInitializeClient(){
        ClientRegistrationHandler handler = ClientRegistrationHandler.get(Pottery.MODID);
        // Wrap all the pot models
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                if(type == PotType.DEFAULT && color == PotColor.BLANK)
                    continue;
                handler.registerBlockModelOverwrite(() -> type.getBlock(color), PotBakedModel::new);
            }
        }
        // Put all decorated pot patterns into the block atlas
        BuiltInRegistries.DECORATED_POT_PATTERNS.registryKeySet().stream()
            .map(DecoratedPotPatterns::location)
            .forEach(texture -> handler.registerAtlasSprite(TextureAtlases.getBlocks(), texture));
    }
}
