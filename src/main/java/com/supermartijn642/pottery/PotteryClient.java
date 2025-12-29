package com.supermartijn642.pottery;

import com.supermartijn642.core.registry.ClientRegistrationHandler;
import com.supermartijn642.core.render.TextureAtlases;
import com.supermartijn642.pottery.content.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotteryClient implements ClientModInitializer {

    @Override
    public void onInitializeClient(){
        ClientRegistrationHandler handler = ClientRegistrationHandler.get(Pottery.MODID);
        // Register block entity renderers
        for(PotType type : PotType.values())
            handler.registerCustomBlockEntityRenderer(type::getBlockEntityType, PotBlockRenderer::new);
        // Wrap all the pot models
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                if(type == PotType.DEFAULT && color == PotColor.BLANK)
                    continue;
                handler.registerBlockModelOverwrite(() -> type.getBlock(color), PotBakedModel::new);
            }
        }
        // Register pot item model
        handler.registerItemModelType("decorated_pot", PotItemModel.CODEC);
        // Put all decorated pot patterns into the block atlas
        BuiltInRegistries.DECORATED_POT_PATTERN.listElements()
            .filter(holder -> holder.key().identifier().getNamespace().equals("minecraft"))
            .map(holder -> holder.value().assetId().withPrefix("entity/decorated_pot/"))
            .forEach(texture -> handler.registerAtlasSprite(TextureAtlases.getBlocks(), texture));
    }
}
