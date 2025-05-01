import com.android.build.api.dsl.Packaging

plugins {
    id("com.android.application")
}

android {
    namespace = "com.nevaloss.sign_lang_app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nevaloss.sign_lang_app"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    fun Packaging.() {
        resources.excludes += setOf("META-INF/*")
        resources.pickFirsts += setOf("META-INF/*")
        jniLibs.keepDebugSymbols += setOf("*.so")
    }

    // THIS is the correct noCompress part
    packaging {
        resources {
            var noCompress = "tflite"
        }
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.3.1")

    implementation ("org.tensorflow:tensorflow-lite:2.8.0") // TensorFlow Lite version
    // If using GPU or hardware acceleration:
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.8.0")
}