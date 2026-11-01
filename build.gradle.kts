plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.publish) apply false
}

subprojects {
    // Publishing into Maven local (a local check, or the Publish workflow's dry run and build step) goes unsigned: only
    // publishToMavenCentral, which has the release key, signs.
    tasks.withType<Sign>().configureEach {
        onlyIf { !project.gradle.startParameter.taskNames.any { "MavenLocal" in it } }
    }
}
