plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Billing product IDs come from gradle.properties (or -P on the command line).
val monthlyProductId: String = (findProperty("uvglow.billing.monthlyProductId") as String?).orEmpty()
val yearlyProductId: String = (findProperty("uvglow.billing.yearlyProductId") as String?).orEmpty()

// AdMob IDs for release builds come from gradle.properties (ADMOB_APP_ID, ADMOB_BANNER_ID).
// Debug builds always use Google's official test IDs, so real ads are never loaded while testing.
val admobAppId: String = (findProperty("ADMOB_APP_ID") as String?).orEmpty().trim()
val admobBannerId: String = (findProperty("ADMOB_BANNER_ID") as String?).orEmpty().trim()
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobBannerId = "ca-app-pub-3940256099942544/6300978111"

android {
    namespace = "com.tubers.blacklight"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tubers.blacklight"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "BILLING_MONTHLY_ID", "\"$monthlyProductId\"")
        buildConfigField("String", "BILLING_YEARLY_ID", "\"$yearlyProductId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Configure a real signingConfig before publishing (see README).

            // A blank banner ID turns ads off (no consent request, no SDK init, no banner). The
            // manifest still needs *some* app ID or the Ads SDK stops the app at launch, so a blank
            // ADMOB_APP_ID falls back to the test app ID, which loads nothing when ads are off.
            manifestPlaceholders["admobAppId"] = admobAppId.ifBlank { testAdmobAppId }
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${if (admobAppId.isBlank()) "" else admobBannerId}\"")
        }
        debug {
            versionNameSuffix = "-debug"
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"$testAdmobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testAdmobBannerId\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.ump)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// PrivacyPolicySyncTest reads these files directly, so a change to either must re-run the tests.
tasks.withType<Test>().configureEach {
    inputs.file(rootProject.file("docs/privacy-policy.md"))
    inputs.file("src/main/res/values/strings.xml")
}
