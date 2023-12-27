package com.supermartijn642.pottery.generators;

import com.supermartijn642.core.generator.ModelGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotType;

/**
 * Created 29/11/2023 by SuperMartijn642
 */
public class PotteryModelGenerator extends ModelGenerator {

    public PotteryModelGenerator(ResourceCache cache){
        super(Pottery.MODID, cache);
    }

    @Override
    public void generate(){
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                if(type == PotType.DEFAULT && color == PotColor.BLANK)
                    continue;
                // Block model
                if(type == PotType.DEFAULT)
                    this.model("block/" + type.getIdentifier() + "/" + type.getIdentifier(color))
                        .parent("block/" + type.getIdentifier())
                        .texture("base", type.getIdentifier() + "/" + type.getIdentifier(color))
                        .particleTexture("#base");
                else
                    this.model("block/" + type.getIdentifier() + "/" + type.getIdentifier(color))
                        .parent("block/" + type.getIdentifier())
                        .texture("base", type.getIdentifier() + "/" + type.getIdentifier(color))
                        .texture("side", type.getIdentifier() + "/" + type.getIdentifier(color) + "_side")
                        .particleTexture("#base");
                // Item model
                this.model("item/" + type.getIdentifier(color))
                    .parent("block/" + type.getIdentifier() + "/" + type.getIdentifier(color));
            }
        }
    }
}
