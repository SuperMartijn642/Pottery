package com.supermartijn642.pottery.generators;

import com.supermartijn642.core.generator.BlockStateGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.pottery.Pottery;
import com.supermartijn642.pottery.content.PotBlock;
import com.supermartijn642.pottery.content.PotColor;
import com.supermartijn642.pottery.content.PotType;

/**
 * Created 29/11/2023 by SuperMartijn642
 */
public class PotteryBlockStateGenerator extends BlockStateGenerator {

    public PotteryBlockStateGenerator(ResourceCache cache){
        super(Pottery.MODID, cache);
    }

    @Override
    public void generate(){
        for(PotType type : PotType.values()){
            for(PotColor color : PotColor.values()){
                if(type == PotType.DEFAULT && color == PotColor.BLANK)
                    continue;
                this.blockState(type.getBlock(color)).variantsForProperty(
                    PotBlock.HORIZONTAL_FACING,
                    (state, variant) -> {
                        int rotation = (int)state.get(PotBlock.HORIZONTAL_FACING).toYRot();
                        variant.model("block/" + type.getIdentifier() + "/" + type.getIdentifier(color), 0, rotation);
                    }
                );
            }
        }
    }
}
