package com.iamkaf.snapshears;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(Constants.MOD_ID)
public class SnapShearsNeoForge {
    public SnapShearsNeoForge(IEventBus eventBus) {
        SnapShearsMod.init();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        SnapShearsMod.onPlayerEntityInteract(
                event.getEntity(), event.getEntity().level(), event.getHand(), event.getTarget());
    }
}
