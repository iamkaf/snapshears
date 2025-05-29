package com.iamkaf.snapshears;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reference {
    public static final String MOD_ID = "snapshears";
    public static final Logger LOGGER = LoggerFactory.getLogger("SnapShears");
    public static final String INITIALIZING_MESSAGE = "SnapShears is initializing...";

    /**
     * Creates resource location in the mod namespace with the given path.
     */
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
