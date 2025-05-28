package com.iamkaf.template;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class Reference {
    public static final String MOD_ID = "template";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String INITIALIZING_MESSAGE = "template is initializing.";

    /**
     * Creates resource location in the mod namespace with the given path.
     */
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
