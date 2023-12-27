package com.supermartijn642.pottery.generators;

import com.supermartijn642.core.generator.LanguageGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotType;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotteryLanguageGenerator extends LanguageGenerator {

    public PotteryLanguageGenerator(ResourceCache cache){
        super(Pottery.MODID, cache, "en_us");
    }

    @Override
    public void generate(){
        // Creative tab
        this.itemGroup(Pottery.ITEM_GROUP, "Pottery");
        // Blocks
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                if(type == PotType.DEFAULT && color == PotColor.BLANK)
                    continue;
                this.block(type.getBlock(color), color == PotColor.BLANK ? type.getTranslation() : color.getTranslation() + " " + type.getTranslation());
            }
        }
    }
}
