package com.supermartijn642.pottery.generators;

import com.supermartijn642.core.generator.ItemInfoGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotItemModel;
import com.supermartijn642.pottery.content.PotType;
import net.minecraft.resources.Identifier;

/**
 * Created 27/12/2024 by SuperMartijn642
 */
public class PotteryItemInfoGenerator extends ItemInfoGenerator {

    public PotteryItemInfoGenerator(ResourceCache cache){
        super(Pottery.MODID, cache);
    }

    @Override
    public void generate(){
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                if(type == PotType.DEFAULT && color == PotColor.BLANK)
                    continue;
                // Item model
                this.info(type.getItem(color))
                    .model(new PotItemModel(Identifier.fromNamespaceAndPath(Pottery.MODID, "block/" + type.getIdentifier() + "/" + type.getIdentifier(color))));
            }
        }
    }
}
