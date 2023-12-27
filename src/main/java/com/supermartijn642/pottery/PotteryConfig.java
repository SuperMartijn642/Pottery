package com.supermartijn642.pottery;

import com.supermartijn642.configlib.api.ConfigBuilders;
import com.supermartijn642.configlib.api.IConfigBuilder;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotteryConfig { // TODO delete if unused

    static{
        IConfigBuilder builder = ConfigBuilders.newTomlConfig(Pottery.MODID, null, false);

        builder.build();
    }

    public static void init(){
        // Cause this class to load
    }
}
