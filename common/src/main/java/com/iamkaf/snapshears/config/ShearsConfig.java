package com.iamkaf.snapshears.config;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.snapshears.Constants;

import java.util.List;

public final class ShearsConfig {
    public static final ConfigHandle HANDLE;
    public static final ConfigValue<Integer> RADIUS;
    public static final ConfigValue<Integer> BONUS_WOOL;
    public static final ConfigValue<Boolean> ONLY_SAME_COLOR;
    public static final ConfigValue<List<String>> SHEARS;

    static {
        ConfigBuilder builder = Konfig.builder(Constants.MOD_ID, "common")
                .scope(ConfigScope.COMMON)
                .syncMode(SyncMode.LOGIN)
                .comment("Gameplay settings for SnapShears.");

        builder.push("general");
        RADIUS = builder.intRange("radius", 3, 0, 16)
                .comment("How far from the sheep you click other sheep can be sheared, in blocks.")
                .sync(true)
                .build();
        BONUS_WOOL = builder.intRange("bonusWool", 0, 0, 3)
                .comment("Extra wool to drop for each additional sheep sheared by snapping.")
                .sync(true)
                .build();
        ONLY_SAME_COLOR = builder.bool("onlySameColor", false)
                .comment("Only snap to sheep that match the first sheep's wool color.")
                .sync(true)
                .build();
        builder.categoryComment("Items and tags that should behave like shears.");
        SHEARS = builder.stringList("shears", List.of("minecraft:shears", "#c:tools/shear"))
                .comment("Add item ids like 'minecraft:shears' or prefix tags with '#', like '#c:tools/shear'.")
                .sync(true)
                .build();
        builder.pop();

        HANDLE = builder.build();
    }

    private ShearsConfig() {
    }

    public static void init() {
    }
}
