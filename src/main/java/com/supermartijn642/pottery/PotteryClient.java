package com.supermartijn642.pottery;

import com.supermartijn642.core.registry.ClientRegistrationHandler;
import com.supermartijn642.pottery.content.PotBlockRenderer;
import com.supermartijn642.pottery.content.PotSpecialModelRenderer;
import com.supermartijn642.pottery.content.PotType;

/**
 * Created 27/11/2023 by SuperMartijn642
 */
public class PotteryClient {

    public static void register(){
        ClientRegistrationHandler handler = ClientRegistrationHandler.get(Pottery.MODID);
        // Register block entity renderers
        for(PotType type : PotType.values())
            handler.registerCustomBlockEntityRenderer(type::getBlockEntityType, PotBlockRenderer::new);
        // Register pot item model
        handler.registerSpecialModelRenderer("decorated_pot", PotSpecialModelRenderer.CODEC);
    }
}
