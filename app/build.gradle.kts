// FILE DENTRO LA CARTELLA APP
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // IMPORTANTE: Se il tuo codice Kotlin ha un package diverso, CAMBIA QUESTO!
    namespace = "com.example.pixelcatalog"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pixelcatalog"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // QUESTA È LA CHIAVE: 1.5.1 funziona perfettamente con Kotlin 1.9.0
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Versioni fisse e stabili
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    
    // Coil e FFmpeg
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.github.wseemann:FFmpegMediaMetadataRetriever-core:1.0.19")
    implementation("com.github.wseemann:FFmpegMediaMetadataRetriever-native:1.0.19")
    
    // Permessi
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}
