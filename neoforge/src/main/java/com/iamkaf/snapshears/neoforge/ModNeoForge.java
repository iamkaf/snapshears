package com.iamkaf.snapshears.neoforge;

import com.iamkaf.snapshears.ModCommon;
import com.iamkaf.snapshears.Reference;
import net.neoforged.fml.common.Mod;

@Mod(Reference.MOD_ID)
public final class ModNeoForge {
    public ModNeoForge() {
        // Run our common setup.
        new ModCommon();
    }
}
