package com.iamkaf.snapshears.config;

import com.iamkaf.konfig.impl.v1.bootstrap.RuntimeEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShearsConfigMigrationTest {
    @TempDir
    Path configDirectory;

    @Test
    void carriesTheOldGeneralSectionIntoTheCurrentLayout() throws IOException {
        Path file = this.configDirectory.resolve("snapshears").resolve("common.toml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                [general]
                radius = 6
                bonusWool = 2
                onlySameColor = true
                shears = ["minecraft:iron_sword", "#minecraft:swords"]

                [__konfig]
                version = 0
                """);
        RuntimeEnvironment.initialize(this.configDirectory, false);

        // Loading the class registers the config, which reads and migrates the file.
        assertEquals(6, ShearsConfig.RADIUS.get());
        assertEquals(2, ShearsConfig.BONUS_WOOL.get());
        assertTrue(ShearsConfig.ONLY_SAME_COLOR.get());
        assertEquals(List.of("minecraft:iron_sword", "#minecraft:swords"), ShearsConfig.SHEARS.get());

        String stored = Files.readString(file);
        assertFalse(stored.contains("bonusWool"), stored);
        assertFalse(stored.contains("onlySameColor"), stored);
        assertTrue(stored.contains("[snapping]"), stored);
        assertTrue(stored.contains("[drops]"), stored);
        assertTrue(stored.contains("[items]"), stored);
    }
}
