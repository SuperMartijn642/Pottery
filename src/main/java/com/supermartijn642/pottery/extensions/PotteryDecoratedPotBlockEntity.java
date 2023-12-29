package com.supermartijn642.pottery.extensions;

import com.supermartijn642.pottery.content.PotBlockEntity;

/**
 * Created 29/12/2023 by SuperMartijn642
 */
public interface PotteryDecoratedPotBlockEntity {

    void potteryWobble(PotBlockEntity.WobbleStyle style);

    long potteryGetWobbleStartedAtTick();

    PotBlockEntity.WobbleStyle potteryGetLastWobbleStyle();
}
