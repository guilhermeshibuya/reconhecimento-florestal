import com.android.build.api.dsl.AaptOptions
import com.android.build.api.dsl.AndroidResources

plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.reconhecimentoflorestal"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.reconhecimentoflorestal"
        minSdk = 28
        targetSdk = 33
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {

    implementation("androidx.camera:camera-core:1.2.3")
    implementation("androidx.camera:camera-camera2:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.herohan:UVCAndroid:1.0.5")
    implementation("com.github.getActivity:XXPermissions:18.3")
    implementation("com.vanniktech:android-image-cropper:4.5.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:latest.release")
//
//    implementation("org.tensorflow:tensorflow-lite:2.5.0")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}