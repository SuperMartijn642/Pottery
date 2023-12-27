package com.supermartijn642.pottery.generators;

import com.supermartijn642.core.generator.AtlasSourceGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;

/**
 * Created 30/11/2023 by SuperMartijn642
 */
public class PotteryAtlasSourceGenerator extends AtlasSourceGenerator {

    public PotteryAtlasSourceGenerator(ResourceCache cache){
        super(Pottery.MODID, cache);
    }

    @Override
    public void generate(){
        // Decorated sides
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                if(type == PotType.DEFAULT)
                    continue;
                this.blockAtlas().texture(type.getIdentifier() + "/" + type.getIdentifier(color) + "_side_decorated");
            }
        }

        // Colored patterns
        for(PotColor color : PotColor.values()){
            if(color == PotColor.BLANK)
                continue;
            BuiltInRegistries.DECORATED_POT_PATTERNS.registryKeySet().stream()
                .filter(key -> key.location().getNamespace().equals("minecraft"))
                .map(DecoratedPotPatterns::location)
                .forEach(texture -> this.blockAtlas().texture("patterns/" + color.getIdentifier() + "/" + texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1)));
        }
    }
}
