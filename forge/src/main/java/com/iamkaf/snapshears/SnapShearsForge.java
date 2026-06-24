package com.iamkaf.snapshears;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class SnapShearsForge {

    public SnapShearsForge() {
        SnapShearsMod.init();
        //? if >=26.1
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(this::onEntityInteractSpecific);
        //? if <26.1
        /*PlayerInteractEvent.EntityInteract.BUS.addListener(this::onEntityInteract);*/
    }

    //? if >=26.1 {
    private boolean onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        SnapShearsMod.onPlayerEntityInteract(
                event.getEntity(), event.getEntity().level(), event.getHand(), event.getTarget());
        return false;
    }
    //?}
    //? if <26.1 {
    /*private boolean onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        SnapShearsMod.onPlayerEntityInteract(
                event.getEntity(), event.getEntity().level(), event.getHand(), event.getTarget());
        return false;
    }
    *///?}
}
