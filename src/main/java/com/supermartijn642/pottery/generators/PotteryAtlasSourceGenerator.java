package com.supermartijn642.pottery.generators;

import com.supermartijn642.core.generator.AtlasSourceGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotType;
import net.minecraft.core.registries.BuiltInRegistries;

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
            BuiltInRegistries.DECORATED_POT_PATTERN.listElements()
                .filter(holder -> holder.key().identifier().getNamespace().equals("minecraft"))
                .map(holder -> holder.value().assetId().withPrefix("entity/decorated_pot/"))
                .forEach(texture -> this.blockAtlas().texture("patterns/" + color.getIdentifier() + "/" + texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1)));
        }
    }
}
