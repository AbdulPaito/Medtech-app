plugins {
    id("com.android.application")
}

android {
    namespace = "com.AbdulPaito.medtrack"
    compileSdk = 34   // ✅ Keep 34 for stability (36 preview may cause resource issues)

    defaultConfig {
        applicationId = "com.AbdulPaito.medtrack"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    buildFeatures {
        viewBinding = true  // ✅ Easier to access XML views safely
    }

    buildFeatures {
        viewBinding = true  // ✅ Easier to access XML views safely
    }
}

dependencies {
    // ✅ Core AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // ✅ Material 3 (for modern design + dark mode)
    implementation("com.google.android.material:material:1.12.0")

    // ✅ Layout and Navigation
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // ✅ Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // ✅ Activity & Lifecycle (recommended for Material3)
    implementation("androidx.activity:activity:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime:2.8.5")

    // ✅ Image Cropping Library
    implementation("com.github.yalantis:ucrop:2.2.8")
    
    // ✅ Glide for Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ✅ Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
