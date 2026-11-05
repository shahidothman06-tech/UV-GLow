# UV Glow — Android (Kotlin + Jetpack Compose)

Package / application ID: **`com.tubers.blacklight`**

UV Glow is a light-effects studio. It draws a 3D-style flashlight with an animated violet beam on the
screen, and can optionally switch on the phone's ordinary white camera LED.

> **Honest hardware note.** A phone screen cannot emit ultraviolet light, and a phone's camera LED is
> plain white light. The app never claims otherwise: the Home screen, the intro and *Settings › About &
> safety* all say so.

---

## Requirements

| Tool | Version |
|---|---|
| Android Studio | a 2026 release that supports AGP 9.4 |
| JDK | 17+ (Android Studio's bundled JDK is fine) |
| Android Gradle Plugin | 9.4.1 (built-in Kotlin — the `kotlin-android` plugin is not applied in `:app`) |
| Gradle (wrapper) | 9.8.2 |
| Kotlin | 2.4.21 (Compose compiler plugin 2.4.21, KSP 2.3.12) |
| compileSdk / targetSdk | 36 (Android 16) |
| minSdk | 26 (Android 8.0) |
| Compose BOM | 2026.06.01 (Compose 1.11.4, Material 3 1.4.0) |
| Google Play Billing | 9.1.0 (`billing-ktx`) |

AndroidX versions are the newest that still build against API 36: Compose 1.12+, core 1.19+,
lifecycle 2.11+ and navigation 2.10+ require compileSdk 37. Move to them together with a
compileSdk 37 bump.

All dependency versions live in `gradle/libs.versions.toml`.

## Build & run

```bash
# From the project root
./gradlew assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # install on a connected device / emulator
./gradlew testDebugUnitTest      # unit tests
```

Or open the folder in Android Studio, let Gradle sync, and press **Run**.

If you see "SDK location not found", create `local.properties` with
`sdk.dir=/path/to/Android/sdk` (Android Studio does this automatically).

## What's inside

| Screen | What works |
|---|---|
| **Home / Light Studio** | Canvas-drawn 3D flashlight; power button; brightness, beam-width and glow-radius sliders; quick chips for Effect, Color, Auto-off and Presets; mode and timer tags |
| **Effects** | Live side preview; 6 modes (UV Glow, Pulse, Light Rays, SOS, Strobe, Blacklight); animation speed 0.5×–2.0×; reduced-motion and strobe-safety callouts; one-time flashing-lights / photosensitive-epilepsy warning before Strobe or SOS first runs |
| **Themes** | 5 colour swatches + HSV custom picker; built-in themes; create / rename / apply / update / delete custom themes |
| **Presets** | Save the current look; preview cards; apply, rename and delete; "Active" state |
| **Auto-off timer** | 1 / 5 / 10 / 30 min; live ring countdown; start, pause, resume and cancel; switches the light (and LED) off when it ends |
| **Settings** | Reduced motion; optional phone LED; Manage subscription (opens Google Play); Rate us (opens the Play Store page); reset settings (with confirm); About & safety with an in-app Privacy policy screen and "Privacy & ad choices" (where consent applies) |
| **Premium** | Free vs Premium table; one monthly plan with Play's localised price, billing period and any trial/intro offer; auto-renewal terms; checkout; restore; subscription status; demo preview switch (debug builds only) |

Free plan limits: 4 modes (Strobe and Blacklight are Premium), 5 presets, 1 custom theme.

**Ads:** one adaptive AdMob banner at the bottom of Effects, Themes, Presets and Settings, only
while the light is off and after Google's consent check (UMP) allows ads. Never on Home, never
over controls, no interstitial or rewarded ads. Debug builds use Google's test IDs.

**Google Play extras** (only for installs from Play, including internal testing; debug or
sideloaded builds skip them quietly):
- **In-app updates** (`play/InAppUpdater.kt`, IMMEDIATE type): when Play has a newer version, a
  full-screen Play screen installs it at launch and the app restarts on the new version.
- **Exit and rating**: Back on Home shows "Press back again to exit"; the second Back may show
  Google Play's rating card first (`play/InAppReview.kt`: not until 3 days after first use, then
  at most once every 14 days, and Play has its own limit), then the app closes.

Every screen adapts to landscape, tablets, foldables and resizable windows: a side navigation rail
in landscape, a two-pane Home with a flashlight stage that scales to fit, and content capped at a
readable width. Android 16 ignores portrait locks on large screens, so the app no longer sets one.

## Architecture

```
com.tubers.blacklight
├── UvGlowApp, MainActivity          app entry, splash, edge-to-edge
├── AppLinks                         privacy policy URL, Play subscription deep link
├── di/AppContainer                  manual dependency container
├── domain/                          pure Kotlin, no Android, fully unit-tested
│   ├── model/                       LightSettings, LightMode, Preset, ThemeItem, TimerState…
│   ├── logic/                       StudioLogic (state transitions), PremiumRules, TimerLogic,
│   │                                ModeAnimation (beam rhythm), ColorMath
│   └── repository/                  repository interfaces
├── data/
│   ├── prefs/                       DataStore: current look + preferences
│   ├── db/                          Room: presets + custom themes (seeded with 3 examples)
│   └── repo/                        Room-backed repositories
├── hardware/                        TorchController (camera LED via CameraManager)
├── ads/                             AdsGateway + GoogleAdsGateway (AdMob + UMP consent), AdPolicy, AdaptiveBanner
├── billing/                         BillingGateway + PlayBillingGateway (Play Billing 9.1), PlanPricing
└── ui/
    ├── state/                       StudioViewModel — the single source of UI state
    ├── navigation/                  NavHost, header, bottom bar, snackbars
    ├── components/                  FlashlightStage, BeamPreview, controls, dialogs, icons
    ├── screens/                     one file per screen
    └── theme/                       design tokens (colours, spacing, radii, type) + accent
```

- **One source of truth.** `StudioViewModel` exposes a single `StateFlow<StudioUiState>`. Every
  action goes through pure functions in `domain/logic`, so the rules are tested in isolation.
- **Instant feedback, gentle persistence.** The look updates in memory immediately and is written to
  DataStore 250 ms after it stops changing (no disk write per slider tick).
- **Timer correctness.** The countdown is computed from a monotonic deadline
  (`SystemClock.elapsedRealtime`), so missed ticks or backgrounding never cause drift. The timer runs
  while the app process is alive, across all screens.
- **Animation cost.** A single frame clock drives the beam and runs only while the light is on, the
  screen is visible (lifecycle STARTED) and reduced motion is off. Reduced motion is honoured from the
  in-app switch *and* the system "Remove animations" setting. Strobe is capped below 3 flashes/s.
- **Accessibility.** Content descriptions on every icon-only control, switch/radio/tab roles, state
  descriptions on sliders and the power button, ≥ 44 dp touch targets, and text that wraps instead of
  being clipped on narrow phones.

## Tests

`app/src/test` contains:

- `TimerLogicTest`: start, pause, resume, cancel, expiry exactly once, irregular ticks, formatting
- `StudioLogicTest`: state transitions, clamping, premium gating, themes, reset, name cleaning
- `PresetLogicTest`: capture, apply, premium-locked presets, active detection, summary text
- `EffectsAndColorTest`: strobe safety cap, mode rhythms, colour maths and contrast
- `StudioViewModelTest`: end-to-end shared state with fakes and virtual time (restore, persistence,
  gating, presets, themes, LED sync, timer expiry, power-off cancels timer, reset, the
  flashing-lights warning, release builds ignoring the demo preview)
- `PlanPricingTest`: offer selection (trial > intro > base plan), billing periods, price and trial wording
- `AppLinksTest`: Play "Manage subscription" deep link
- `AdPolicyTest`: no banner while the light is on (any mode), without consent, before the SDK is ready, or on Home
- `RoutesTest`: which screens get the back-button header (Timer, Premium, Privacy policy)
- `PrivacyPolicySyncTest`: the in-app policy (strings.xml) and docs/privacy-policy.md say the same thing

## Hardware: phone LED

*Settings › Phone flashlight (LED)* is optional and off by default. It uses
`CameraManager.setTorchMode`, which needs **no camera permission**. It checks
`FEATURE_CAMERA_FLASH` and `FLASH_INFO_AVAILABLE`, prefers the rear camera, reports errors such as
"camera busy in another app", and turns the LED off when the light turns off, when the timer ends, or
when you leave the app.

## Billing / Premium

Play Billing Library 9.1 (`PlayBillingGateway`):

- Pending purchases enabled with `PendingPurchasesParams`; `enableAutoServiceReconnection()`.
- Product details come back as `QueryProductDetailsResult`; products Play can't return
  (`unfetchedProductList`) show as "Not available" instead of silently missing.
- For each plan the app picks the offer with a free trial, else an intro price, else the base plan,
  and shows Play's formatted price and billing period (`PlanPricing`, unit-tested). Nothing is
  hard-coded.
- Owned subscriptions are re-read with `queryPurchasesAsync` every time the app connects (restore
  on launch) and from *Restore purchases*. Purchases are acknowledged.
- *Settings › Manage subscription* opens
  `https://play.google.com/store/account/subscriptions?sku=<productId>&package=com.tubers.blacklight`
  (the plain subscriptions list when nothing is active).

**Debug builds** without product IDs run a clearly labelled demo: "Continue · demo checkout" never
charges, and *Preview Premium (demo)* unlocks features locally. **Release builds** never show either.
A release built **without** `uvglow.billing.monthlyProductId` hides Premium completely (no crown,
paywall or Manage subscription; `BuildConfig.PREMIUM_ENABLED`). With an ID set, Premium shows; if
Google Play Billing is unavailable the Premium screen shows an error and no checkout. A preview flag
left over from a debug install is ignored.

To go live:

1. Create one **subscription** in Play Console (monthly), with an active base plan
   (and optionally a free-trial or intro offer).
2. Put its product ID in `gradle.properties`:
   ```properties
   uvglow.billing.monthlyProductId=uvglow_premium_monthly
   ```
3. Upload a signed build to an internal testing track and add licence testers.
4. **Recommended:** verify purchase tokens on your own server (Play Developer API) before granting
   entitlement. The app acknowledges purchases client-side, which is Play's minimum requirement.

## Language

The language screen (first launch and *Settings › Language*) only appears in **debug** builds for
now, because the UI text isn't translated yet. Once the strings are moved to `strings.xml` and
translated, set `LANGUAGE_PICKER_ENABLED` to `true` for release in `app/build.gradle.kts`.

## Release build (GitHub Actions)

First run `powershell -ExecutionPolicy Bypass -File scripts\make-upload-keystore.ps1` on Windows: it
asks for the alias, password and product ID, creates `%USERPROFILE%\upload-keystore.jks` plus a
base64 copy, sets the product ID in `gradle.properties`, and lists the secrets below to paste in.

`.github/workflows/release-aab.yml` builds the signed `.aab` for Play. Run it from
*Actions › Release AAB › Run workflow* (or push a `v*` tag) and download the **UVGlow-release-aab**
artifact. It needs these in *Settings › Secrets and variables › Actions*:

| Name | Kind | Value |
|---|---|---|
| `UVGLOW_KEYSTORE_BASE64` | secret | the upload keystore, base64 on one line |
| `UVGLOW_STORE_PASSWORD` | secret | keystore password |
| `UVGLOW_KEY_ALIAS` | secret | key alias |
| `UVGLOW_KEY_PASSWORD` | secret | key password |
| `UVGLOW_MONTHLY_PRODUCT_ID` | variable | optional; overrides the product ID in `gradle.properties` |
| `ADMOB_APP_ID`, `ADMOB_BANNER_ID` | variable | real AdMob IDs (blank ships without ads) |

`versionCode` is 100 + the workflow run number unless you type one when starting the run; it must be
higher than the last upload. The R8 mapping file is uploaded as **UVGlow-release-mapping** (attach it
in Play Console for readable crash reports). Local release builds read the same values from
`~/.gradle/gradle.properties` (`UVGLOW_STORE_FILE` etc.).

## Production release checklist

- [ ] Create an upload keystore (never commit it) and add it as the signing secrets above.
- [ ] Bump `versionName` in `app/build.gradle.kts` for each release (CI sets `versionCode`).
- [x] Launcher icon: vector adaptive icon (`drawable/ic_launcher_foreground.xml`, dark background,
      `ic_launcher_monochrome.xml` for themed icons) redrawn from `docs/icon-512.png`, artwork inside
      the 66dp safe zone. Upload `docs/icon-512.png` as the Play Store icon (512 × 512 PNG).
- [ ] Billing set up as above. Test a purchase, a free trial, restore and *Manage subscription*
      with licence testers.
- [ ] **Privacy policy:** published by GitHub Pages (Settings › Pages › Deploy from a branch ›
      `main` › `/docs`) at https://shahidothman06-tech.github.io/UV-GLow/privacy-policy (=
      `AppLinks.PRIVACY_POLICY_URL`). Enter that URL in Play Console › App content › Privacy policy.
      Edit `strings.xml` and `docs/privacy-policy.md` together; `PrivacyPolicySyncTest` checks they match.
- [ ] **AdMob IDs:** create the app and an adaptive banner ad unit in AdMob, then set `ADMOB_APP_ID`
      and `ADMOB_BANNER_ID` in `gradle.properties` (release only; debug always uses Google's test
      IDs). With either blank, the release build ships with no ads and doesn't crash.
- [ ] AdMob › Privacy & messaging: publish a **GDPR (EEA, UK, Switzerland) consent message** for
      the app; the app's UMP flow shows it before any ad loads.
- [ ] Play Console › App content › **Ads: "Yes, my app contains ads"**.
- [ ] Play Console › App content › **Advertising ID**: declare it is used for advertising.
- [ ] **Data safety form:** data is **collected and shared** for **Advertising or marketing** (and
      fraud prevention) by the AdMob SDK: **Device or other IDs** (advertising ID) and
      **Approximate location** (from IP address); also App interactions and diagnostics if AdMob's
      current Data safety guidance lists them. Not collected: settings, presets and themes (on device
      only). Purchases are processed by Google Play.
- [ ] **app-ads.txt:** add your developer website in Play Console (store listing contact details)
      and publish `app-ads.txt` at its root with the line AdMob gives you
      (`google.com, pub-XXXXXXXXXXXXXXXX, DIRECT, f08c47fec0942fa0`).
- [ ] Content rating questionnaire: mention the optional flashing (Strobe/SOS) effects.
- [ ] Optional: bundle the design's fonts (Sora, JetBrains Mono) under `res/font` and set them in
      `ui/theme/Theme.kt` (the app uses the system sans-serif and monospace today).
- [ ] Play listing: describe the app as a *screen light effect*. Do not claim UV output, stain
      detection, banknote checking or disinfection.
- [ ] Check the listing's screenshots include tablet / landscape layouts (large-screen quality).
- [ ] `./gradlew bundleRelease` and test the R8-minified build before uploading.
- [ ] **New personal developer account?** Google requires a **closed test with at least 12
      testers opted in for 14 continuous days** before you can apply for production access.
      Start it early: Play Console › Testing › Closed testing.

## Known limitations

- The timer lives in the app process. If Android kills the app in the background, the timer stops
  (the light can't be on then anyway). A foreground service would be needed for a timer that only
  controls the LED with the app closed.
- Soft beam edges use layered gradients rather than real blur, so they look the same on API 26–35.
