plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    id("kotlin-kapt") // Maintaining KAPT for Nudi as originally written
}

android {
    namespace = "com.example.nudiconverter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nudiconverter"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/services/javax.xml.stream.XMLEventFactory"
            excludes += "META-INF/services/javax.xml.stream.XMLInputFactory"
            excludes += "META-INF/services/javax.xml.stream.XMLOutputFactory"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)
    
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    
    // PDF & DOCX (Manual versions as they are specific to this module)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    
    // Apache POI
    implementation("com.fasterxml:aalto-xml:1.3.2")
    implementation("org.apache.poi:poi-ooxml:5.2.5") {
        exclude(group = "xml-apis", module = "xml-apis")
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    }
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.20.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
