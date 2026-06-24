package com.iamkaf.snapshears;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

/**
 * Fabric entry point.
 */
public class SnapShearsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SnapShearsMod.init();
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
                SnapShearsMod.onPlayerEntityInteract(player, level, hand, entity));
    }
}
