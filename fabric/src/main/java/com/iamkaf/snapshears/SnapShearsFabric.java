package com.iamkaf.snapshears;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric entry point.
 */
public class SnapShearsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SnapShearsMod.init();
    }
}
