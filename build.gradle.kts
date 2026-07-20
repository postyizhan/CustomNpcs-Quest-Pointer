
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// devOnlyNonPublishable puts CustomNPC-Plus on the compile/runtime classpath, but
// RetroFuturaGradle marks it as a "known library" so FML's mod-jar scanner skips it -
// it never shows up as a loaded mod in runClient/runServer without also being a real
// file inside the run's mods/ folder. Copy it there before every run task.
val copyCustomNpcsToRunMods by tasks.registering(Copy::class) {
    from(file("libs/CustomNPC-Plus-1.11.1.jar"))
    into(file("run/client/mods"))
}

val copyCustomNpcsToServerRunMods by tasks.registering(Copy::class) {
    from(file("libs/CustomNPC-Plus-1.11.1.jar"))
    into(file("run/server/mods"))
}

tasks.matching { it.name.startsWith("runClient") }.configureEach {
    dependsOn(copyCustomNpcsToRunMods)
}

tasks.matching { it.name.startsWith("runServer") }.configureEach {
    dependsOn(copyCustomNpcsToServerRunMods)
}
