import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    alias(libs.plugins.navigation.safeargs)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")

}

android {
    namespace = "com.martinez.simulago"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.martinez.simulago"
        minSdk = 29
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        // Asegúrate de que esta línea esté presente y sea 'true'
        buildConfig = true
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
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            // 'this' se refiere a la salida, que es nuestro APK
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val project = "SimulaGo" // El nombre de tu app
            val aapt_version_code = variant.versionCode // El versionCode desde defaultConfig
            val version_name = variant.versionName // El versionName desde defaultConfig
            val newApkName = "$project-v$version_name-($aapt_version_code).apk"
            output.outputFileName = newApkName
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures{
        viewBinding = true
    }
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // == ARQUITECTURA (MVVM) & CICLO DE VIDA - EL CEREBRO DE TU APP ==
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    // == FIREBASE (Opcional pero recomendado para tus planes) ==
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    // == TESTING - PARA ASEGURAR LA CALIDAD DE TU CÓDIGO ==
    testImplementation(libs.junit)
    // Pruebas de Instrumentación (se ejecutan en un dispositivo/emulador)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.navigation.ui.ktx)
    val room_version = "2.7.2"
    implementation(libs.androidx.room.runtime)
    implementation(libs.room.ktx) // Soporte para Coroutines y Flow
    ksp("androidx.room:room-compiler:$room_version")
    val hilt_version = "2.57"
    implementation(libs.hilt.android)
    ksp("com.google.dagger:hilt-android-compiler:$hilt_version")

    implementation("androidx.preference:preference-ktx:1.2.1")
    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    // Opcional pero recomendado: un interceptor para logging
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")


}