package com.iamkaf.template.neoforge;

import com.iamkaf.template.ModCommon;
import com.iamkaf.template.Reference;
import net.neoforged.fml.common.Mod;

@Mod(Reference.MOD_ID)
public final class ModNeoForge {
    public ModNeoForge() {
        // Run our common setup.
        new ModCommon();
    }
}
