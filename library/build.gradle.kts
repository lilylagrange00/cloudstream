import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("maven-publish")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.dokka)
}

val javaTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())

kotlin {
    version = "1.0.1"

    androidTarget()
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

        commonMain.dependencies {
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

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        jvmTarget.set(javaTarget)
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

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    namespace = "com.lagradost.api"

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(javaTarget.target)
        targetCompatibility = JavaVersion.toVersion(javaTarget.target)
    }

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }
}

/* ------------------------------
   DOKKA Javadoc JAR (for JitPack)
   ------------------------------ */
tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml.get().outputDirectory)
}

dokka {
    moduleName = "Library"
    dokkaSourceSets {
        configureEach {
            analysisPlatform = KotlinPlatform.AndroidJVM
            documentedVisibilities(
                VisibilityModifier.Public,
                VisibilityModifier.Protected
            )
            sourceLink {
                localDirectory = file("..")
                remoteUrl("https://github.com/recloudstream/cloudstream/tree/master")
                remoteLineSuffix = "#L"
            }
        }
    }
}

/* ------------------------------
   Publishing (JitPack compatible)
   ------------------------------ */
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.lagradost.api"
            artifactId = "library"
            version = "1.0"

            afterEvaluate {
                from(components["release"])
            }

            // Attach Dokka HTML javadoc jar
            artifact(tasks["javadocJar"])
        }
    }
}
