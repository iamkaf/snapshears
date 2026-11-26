package com.iamkaf.snapshears;

import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

final class SnapShearsForgeClient {
    private SnapShearsForgeClient() {
    }

    static void init() {
        KonfigForgeClientScreens.register(Constants.MOD_ID);
    }
}
