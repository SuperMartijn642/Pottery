package com.supermartijn642.pottery.generators;

import com.supermartijn642.core.generator.AtlasSourceGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.core.generator.ResourceGenerator;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotColor;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.stream.Stream;

/**
 * Created 30/11/2023 by SuperMartijn642
 */
public class PotteryAtlasSourceGenerator extends AtlasSourceGenerator {

    public PotteryAtlasSourceGenerator(ResourceCache cache){
        super(Pottery.MODID, cache);
    }

    @Override
    public void generate(){
        // Pattern textures
        AtlasBuilder decoratedPotAtlas = this.atlas(Sheets.DECORATED_POT_SHEET);
        for(PotColor color : PotColor.values()){
            if(color == PotColor.BLANK)
                continue;
            Stream.concat(
                    ResourceGenerator.registryAccess.lookupOrThrow(Registries.DECORATED_POT_PATTERN).listElements()
                        .filter(holder -> holder.key().identifier().getNamespace().equals("minecraft"))
                        .map(holder -> holder.value().assetId()),
                    Stream.of(Identifier.withDefaultNamespace("decorated_pot_side"))
                )
                .map(id -> id.getPath().substring(id.getPath().lastIndexOf('/') + 1))
                .map(name -> "patterns/" + color.getIdentifier() + "/" + name)
                .forEach(decoratedPotAtlas::texture);
        }
    }
}
