import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// Configuración de firma externalizada (no versionada).
// Copia keystore.properties.example a keystore.properties y completa tus valores.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}
val hasSigningConfig = keystorePropertiesFile.exists()

// Credencial SMTP para el reporte de errores por correo (opcional; sin ella el build
// igual compila y el envío queda deshabilitado). Prioridad: variable de entorno
// UNITECH_SMTP_PASSWORD -> propiedad Gradle -PunitechSmtpPassword -> unitechSmtpPassword
// en local.properties (gitignored, nunca versionada) -> vacío.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

val smtpPassword: String = System.getenv("UNITECH_SMTP_PASSWORD")
    ?: (project.findProperty("unitechSmtpPassword") as String?)
    ?: (localProperties["unitechSmtpPassword"] as String?)
    ?: ""

val smtpPasswordEscaped = smtpPassword.replace("\\", "\\\\").replace("\"", "\\\"")

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

val majorVersion = 1
val minorVersion = 0
val patchVersion = 0

val versionCodeResult = majorVersion * 10000 + minorVersion * 100 + patchVersion
val versionNameString = "$majorVersion.$minorVersion.$patchVersion"

android {
    namespace = "com.diprotec.inventario"
    buildFeatures { buildConfig = true }
    compileSdk = 34

    defaultConfig {
        applicationId = "com.diprotec.inventario"
        minSdk = 26
        targetSdk = 30
        versionCode = versionCodeResult
        versionName = versionNameString

        buildConfigField("String", "SMTP_PASSWORD", "\"$smtpPasswordEscaped\"")
    }

    signingConfigs {
        create("release") {
            if (hasSigningConfig) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("free") {
            dimension = "version"
            val appName = "Inventario"
            manifestPlaceholders["appName"] = appName
        }
    }

    lint {
        disable += setOf("ExpiredTargetSdkVersion")
        abortOnError = false
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasSigningConfig) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = "unitech_520_inventario_$versionNameString.apk"
        }
    }


    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"

        resources.excludes += "META-INF/LICENSE.md"
        resources.excludes += "META-INF/LICENSE-notice.md"
        resources.excludes += "META-INF/NOTICE.md"
        resources.excludes += "META-INF/NOTICE"
        resources.excludes += "META-INF/LICENSE"

        // com.sun.mail:android-mail y android-activation traen archivos de proveedores
        // duplicados bajo META-INF/; sin esto el empaquetado falla con
        // "More than one file was found with OS independent path".
        resources.pickFirsts += "META-INF/javamail.providers"
        resources.pickFirsts += "META-INF/javamail.default.providers"
        resources.pickFirsts += "META-INF/javamail.address.map"
        resources.pickFirsts += "META-INF/javamail.default.address.map"
        resources.pickFirsts += "META-INF/mailcap"
        resources.pickFirsts += "META-INF/mailcap.default"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.material:material-icons-extended")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // WorkManager + Hilt Work
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Retrofit + Moshi + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.moshi:moshi-adapters:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Cámara
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // SDK local
    implementation(files("libs/UnitechSDK_1.2.13.jar"))

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Reporte de errores por correo (SMTP directo, sin backend de telemetría propio)
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
}
