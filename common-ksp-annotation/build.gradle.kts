plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()

    js(IR) {
        moduleName = "SharedAnnotations"
        version = "0.0.5"
        browser {
            webpackTask {
                output.libraryTarget = "umd"
            }
        }
        binaries.executable()
        generateTypeScriptDefinitions()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
    }
}
