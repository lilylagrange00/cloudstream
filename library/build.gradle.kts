plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
    id("com.codingfeline.buildkonfig")
    id("org.jetbrains.dokka")
}

kotlin {
    androidTarget()
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.nicehttp)
                implementation(libs.jackson.module.kotlin)
            }
        }
    }
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    namespace = "com.lagradost.api"
}

buildkonfig {
    packageName = "com.lagradost.api"
    defaultConfigs {
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "DEBUG", "true")
    }
}

// ---------------- Dokka v2 ----------------
tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.named("dokkaHtml"))
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaHtml").get().outputs.files)
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets["commonMain"].kotlin)
}

// ---------------- Publishing ----------------
publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["kotlin"])
            artifact(tasks.named("sourcesJar").get())
            artifact(tasks.named("javadocJar").get())
            groupId = "com.lagradost.api"
            artifactId = "library"
            version = "1.0"
        }
    }
}
