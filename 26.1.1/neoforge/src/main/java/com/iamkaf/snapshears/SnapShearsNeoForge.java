package com.iamkaf.snapshears;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class SnapShearsNeoForge {
    public SnapShearsNeoForge(IEventBus eventBus) {
        SnapShearsMod.init();
    }
}