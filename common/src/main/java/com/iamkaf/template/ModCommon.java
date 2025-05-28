package com.iamkaf.template;

import com.iamkaf.amber.api.core.AmberMod;
import com.iamkaf.template.registry.CreativeModeTabs;
import com.iamkaf.template.registry.Items;

public class ModCommon extends AmberMod {
    public ModCommon() {
        super(Reference.MOD_ID);
        Reference.LOGGER.info(Reference.INITIALIZING_MESSAGE);

        // Registries
        Items.init();
        CreativeModeTabs.init();
    }
}
