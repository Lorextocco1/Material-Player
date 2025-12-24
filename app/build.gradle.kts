plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // CAMBIA QUESTO SE IL TUO PACKAGE È DIVERSO (es. com.example.materialplayer)
    namespace = "com.example.pixelcatalog"
    compileSdk = 34

    defaultConfig {
        // ANCHE QUESTO DEVE COINCIDERE COL NAMESPACE
        applicationId = "com.example.pixelcatalog"
        minSdk = 26 // Android 8.0 (Oreo)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        // Questa versione è specifica per Kotlin 1.9.0
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- INTERFACCIA UTENTE (Jetpack Compose) ---
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // --- LOGICA E ATTIVITÀ ---
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // --- ICONE ---
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // --- CARICAMENTO IMMAGINI (Coil) ---
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- MOTORE FFMPEG (Per leggere MKV/AV1/VP9) ---
    implementation("com.github.wseemann:FFmpegMediaMetadataRetriever-core:1.0.19")
    implementation("com.github.wseemann:FFmpegMediaMetadataRetriever-native:1.0.19")

    // --- GESTIONE PERMESSI ---
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}