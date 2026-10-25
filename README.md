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
| **Settings** | Reduced motion; optional phone LED; Manage subscription (opens Google Play); replay intro; reset settings (with confirm); About & safety with an in-app Privacy policy screen |
| **Premium** | Free vs Premium table; monthly and yearly plans with Play's localised price, billing period and any trial/intro offer; auto-renewal terms; checkout; restore; subscription status; demo preview switch (debug builds only) |

Free plan limits: 4 modes (Strobe and Blacklight are Premium), 5 presets, 1 custom theme.

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
If product IDs are missing or Google Play Billing is unavailable, the Premium screen shows an error
and no checkout. A preview flag left over from a debug install is ignored.

To go live:

1. Create two **subscriptions** in Play Console (monthly and yearly), each with an active base plan
   (and optionally a free-trial or intro offer).
2. Put their product IDs in `gradle.properties`:
   ```properties
   uvglow.billing.monthlyProductId=uvglow_premium_monthly
   uvglow.billing.yearlyProductId=uvglow_premium_yearly
   ```
3. Upload a signed build to an internal testing track and add licence testers.
4. **Recommended:** verify purchase tokens on your own server (Play Developer API) before granting
   entitlement. The app acknowledges purchases client-side, which is Play's minimum requirement.

## Production release checklist

- [ ] Create an upload keystore and add a `signingConfig` for `release` (never commit the keystore).
- [ ] Bump `versionCode` / `versionName` in `app/build.gradle.kts`.
- [ ] Replace launcher icons with a high-resolution export (the included icons are generated from the
      264 px design asset). Android Studio › *New › Image Asset* creates every density.
- [ ] Billing set up as above. Test a purchase, a free trial, restore and *Manage subscription*
      with licence testers.
- [ ] **Privacy policy:** set `AppLinks.CONTACT_EMAIL` and the same address in `docs/privacy-policy.md`
      (`PrivacyPolicySyncTest` checks the two copies match), publish the page at a public https URL, set `AppLinks.PRIVACY_POLICY_URL`
      to that URL, and enter the same URL in Play Console › App content › Privacy policy.
- [ ] **Data safety form:** "No data collected" and "No data shared". Settings stay on the device
      (and in Android backup); purchases are processed by Google Play. Check this still holds
      if you add any SDK.
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
