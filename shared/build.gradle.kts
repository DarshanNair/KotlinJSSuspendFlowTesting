plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
}

kotlin {
    js(IR) {
        moduleName = "SharedJS"
        browser {
            webpackTask {
                mainOutputFileName = "KmpEventManager.js"
            }
        }
        binaries.executable()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":common-ksp-annotation"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            }
        }
    }
}

dependencies {
    add("kspJs", project(":common-ksp-processor"))
}
