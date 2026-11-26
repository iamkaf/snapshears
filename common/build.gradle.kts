plugins {
    id("com.iamkaf.multiloader.common")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
    // Konfig keeps its config parser out of the artifact the mod compiles against.
    testRuntimeOnly("com.electronwill.night-config:core:3.8.3")
    testRuntimeOnly("com.electronwill.night-config:toml:3.8.3")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// The config test loads Konfig and Amber from the compile classpath and needs no game bootstrap.
sourceSets.test {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().compileClasspath
}
