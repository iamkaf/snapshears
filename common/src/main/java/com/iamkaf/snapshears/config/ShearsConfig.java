package com.iamkaf.snapshears.config;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ShearsConfig {
    public static final @Nullable String HEADER_COMMENT =
            "// SnapShears configuration\n" + "// Add item ids like 'minecraft:shears' or prefix a tag with '#' (e.g. " +
                    "'#c:tools/shear').\n";
    /**
     * List of item or tag identifiers that should behave as shears.
     * <p>
     * Add item ids like "coolshears:epic_shears" or prefix an item tag with '#'
     * to use the items from that tag (e.g. "#c:tools/shear").
     */
    public List<String> shears = new ArrayList<>(List.of("minecraft:shears", "#c:tools/shear"));
}
