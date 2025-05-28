package com.iamkaf.template.forge;

import com.iamkaf.template.ModCommon;
import com.iamkaf.template.Reference;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Reference.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModForge {
    public ModForge() {
        EventBuses.registerModEventBus(Reference.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        new ModCommon();
    }
}
