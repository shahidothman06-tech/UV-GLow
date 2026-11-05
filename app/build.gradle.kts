plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Billing product IDs come from gradle.properties (or -P on the command line).
val monthlyProductId: String = (findProperty("uvglow.billing.monthlyProductId") as String?).orEmpty().trim()

// Play needs a higher versionCode for every upload. CI passes -Puvglow.versionCode (from the run
// number); local builds fall back to the value below.
val appVersionCode: Int = (findProperty("uvglow.versionCode") as String?)?.trim()?.toIntOrNull() ?: 2

// AdMob IDs for release builds come from gradle.properties (ADMOB_APP_ID, ADMOB_BANNER_ID).
// Debug builds always use Google's official test IDs, so real ads are never loaded while testing.
val admobAppId: String = (findProperty("ADMOB_APP_ID") as String?).orEmpty().trim()
val admobBannerId: String = (findProperty("ADMOB_BANNER_ID") as String?).orEmpty().trim()
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobBannerId = "ca-app-pub-3940256099942544/6300978111"

// Upload-key signing for release builds. The values live outside the repo, in
// C:\Users\Admin\.gradle\gradle.properties (UVGLOW_STORE_FILE, UVGLOW_STORE_PASSWORD,
// UVGLOW_KEY_ALIAS, UVGLOW_KEY_PASSWORD). Debug builds never need them.
fun secret(name: String): String? = (findProperty(name) as String?)?.trim()?.takeIf { it.isNotEmpty() }
val uploadStoreFile = secret("UVGLOW_STORE_FILE")
val uploadStorePassword = secret("UVGLOW_STORE_PASSWORD")
val uploadKeyAlias = secret("UVGLOW_KEY_ALIAS")
val uploadKeyPassword = secret("UVGLOW_KEY_PASSWORD")
val hasUploadKey = listOf(uploadStoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword).all { it != null } &&
    file(uploadStoreFile!!).isFile

// Release-only warnings, so a release isn't built by accident without signing or ads.
if (gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }) {
    if (!hasUploadKey) {
        logger.warn("WARNING: UV Glow release is NOT signed - set UVGLOW_STORE_FILE, UVGLOW_STORE_PASSWORD, " +
            "UVGLOW_KEY_ALIAS and UVGLOW_KEY_PASSWORD in ~/.gradle/gradle.properties (and check the keystore exists).")
    }
    if (monthlyProductId.isBlank()) {
        logger.warn("WARNING: uvglow.billing.monthlyProductId is blank - this UV Glow release hides Premium entirely.")
    }
    if (admobAppId.isBlank() || admobBannerId.isBlank()) {
        logger.warn("WARNING: ADMOB_APP_ID or ADMOB_BANNER_ID is blank - this UV Glow release will show NO ads. " +
            "Set both in gradle.properties to enable AdMob.")
    }
}

android {
    namespace = "com.tubers.blacklight"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tubers.blacklight"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "BILLING_MONTHLY_ID", "\"$monthlyProductId\"")
        // Premium (crown, paywall, Manage subscription) shows in release only once a real product ID is set.
        buildConfigField("boolean", "PREMIUM_ENABLED", "${monthlyProductId.isNotBlank()}")
        // No translations yet: release builds skip the language screen. Set true once strings are translated.
        buildConfigField("boolean", "LANGUAGE_PICKER_ENABLED", "false")
    }

    signingConfigs {
        if (hasUploadKey) {
            create("upload") {
                storeFile = file(uploadStoreFile!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
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
            // Signed with the upload key when it's configured; otherwise unsigned (with a warning).
            if (hasUploadKey) signingConfig = signingConfigs.getByName("upload")

            // A blank banner ID turns ads off (no consent request, no SDK init, no banner). The
            // manifest still needs *some* app ID or the Ads SDK stops the app at launch, so a blank
            // ADMOB_APP_ID falls back to the test app ID, which loads nothing when ads are off.
            manifestPlaceholders["admobAppId"] = admobAppId.ifBlank { testAdmobAppId }
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${if (admobAppId.isBlank()) "" else admobBannerId}\"")
        }
        debug {
            versionNameSuffix = "-debug"
            // Debug always shows Premium (demo checkout without a product ID) and the language screen.
            buildConfigField("boolean", "PREMIUM_ENABLED", "true")
            buildConfigField("boolean", "LANGUAGE_PICKER_ENABLED", "true")
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
    // Play Services (Ads/UMP/Billing via play-services-basement) pulls in androidx.fragment 1.1.0,
    // which Play Console flags as outdated. Declaring a current fragment-ktx overrides it.
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
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
    implementation(libs.play.app.update.ktx)
    implementation(libs.play.review.ktx)

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
