package com.iamkaf.snapshears;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

public class SnapShears implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            return CommonClass.onPlayerEntityInteract(player, level, hand, entity);
        });
    }
}
