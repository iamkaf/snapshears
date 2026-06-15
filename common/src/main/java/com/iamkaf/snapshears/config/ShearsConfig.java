package com.iamkaf.snapshears.config;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigMigrationContext;
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
                .syncMode(SyncMode.LOGIN_AND_RELOAD)
                .schemaVersion(1)
                .migrate(0, ShearsConfig::migrateFlatConfig)
                .comment("SnapShears gameplay settings.")
                .info(info -> info
                        .headerKey("snapshears.config.info.common.header")
                        .inlineTextKey("snapshears.config.info.common.text")
                        .urlKey("snapshears.config.info.report_issue", "https://github.com/iamkaf/snapshears"));

        builder.push("snapping")
                .categoryComment("Rules for how far SnapShears reaches and which sheep belong in the same burst.")
                .categoryInfo(info -> info
                        .headerKey("snapshears.config.info.snapping.header")
                        .inlineTextKey("snapshears.config.info.snapping.text"))
                .headerKey("snapshears.config.info.snapping.header");
        RADIUS = builder.intRange("radius", 3, 0, 16)
                .comment("How far from the sheep you click other sheep can be sheared, in blocks.")
                .info(info -> info.inlineTextKey("snapshears.config.radius.info"))
                .sync(true)
                .build();
        ONLY_SAME_COLOR = builder.bool("only_same_color", false)
                .comment("Only snap to sheep that match the first sheep's wool color.")
                .info(info -> info.inlineTextKey("snapshears.config.only_same_color.info"))
                .sync(true)
                .build();
        builder.pop();

        builder.push("drops")
                .categoryComment("Extra wool rewards for sheep sheared by a SnapShears burst.")
                .categoryInfo(info -> info
                        .headerKey("snapshears.config.info.drops.header")
                        .inlineTextKey("snapshears.config.info.drops.text"))
                .headerKey("snapshears.config.info.drops.header");
        BONUS_WOOL = builder.intRange("bonus_wool", 0, 0, 3)
                .comment("Extra wool to drop for each additional sheep sheared by snapping.")
                .info(info -> info.inlineTextKey("snapshears.config.bonus_wool.info"))
                .sync(true)
                .build();
        builder.pop();

        builder.push("items")
                .categoryComment("Items and tags that should behave like shears.")
                .categoryInfo(info -> info
                        .headerKey("snapshears.config.info.items.header")
                        .inlineTextKey("snapshears.config.info.items.text"))
                .headerKey("snapshears.config.info.items.header");
        SHEARS = builder.stringList("shears", List.of("minecraft:shears", "#c:tools/shear"))
                .comment("Add item ids like 'minecraft:shears' or prefix tags with '#', like '#c:tools/shear'.")
                .info(info -> info.inlineTextKey("snapshears.config.shears.info"))
                .sync(true)
                .build();
        builder.pop();

        HANDLE = builder.build();
    }

    private static void migrateFlatConfig(ConfigMigrationContext context) {
        context.rename("general.radius", "snapping.radius");
        context.rename("general.bonusWool", "drops.bonus_wool");
        context.rename("general.onlySameColor", "snapping.only_same_color");
        context.rename("general.shears", "items.shears");
    }

    private ShearsConfig() {
    }

    public static void init() {
    }
}
