import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    id("maven-publish")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.dokka)
}

val javaTarget = libs.versions.jvmTarget.get()

kotlin {
    androidTarget() // required for Android
    jvm()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xannotation-default-target=param-property"
        )
    }

    sourceSets {
        all {
            languageSettings.optIn("com.lagradost.cloudstream3.Prerelease")
        }

        val commonMain by getting {
            dependencies {
                implementation(libs.nicehttp)
                implementation(libs.jackson.module.kotlin)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.fuzzywuzzy)
                implementation(libs.jsoup)
                implementation(libs.rhino)
                implementation(libs.newpipeextractor)
                implementation(libs.tmdb.java)
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = javaTarget
    }
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    namespace = "com.lagradost.api"

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(javaTarget)
        targetCompatibility = JavaVersion.toVersion(javaTarget)
    }
}

buildkonfig {
    packageName = "com.lagradost.api"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        val isDebug = kotlin.runCatching { extra.get("isDebug") }.getOrNull() == true
        buildConfigField(FieldSpec.Type.BOOLEAN, "DEBUG", isDebug.toString())

        val localProperties = gradleLocalProperties(rootDir, project.providers)
        buildConfigField(
            FieldSpec.Type.STRING,
            "MDL_API_KEY",
            (System.getenv("MDL_API_KEY") ?: localProperties["mdl.key"]).toString()
        )
    }
}

// --- Dokka v2 setup ---
tasks.register("dokkaHtml") {
    group = "documentation"
    doLast {
        println("Run Dokka with ./gradlew dokkaHtml")
    }
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaHtml"))
    archiveClassifier.set("javadoc")
    // Dokka v2 outputs are in build/dokka/html
    from(layout.buildDirectory.dir("dokka/html"))
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets["commonMain"].kotlin)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.lagradost.api"
            artifactId = "library"
            version = "1.0"

            from(components["kotlin"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
        }
    }
}
