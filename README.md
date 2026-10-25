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
| Android Studio | Ladybug (2024.2.1) or newer |
| JDK | 17+ (Android Studio's bundled JDK is fine) |
| Android Gradle Plugin | 8.7.3 |
| Gradle (wrapper) | 8.11.1 |
| Kotlin | 2.1.0 (Compose compiler plugin 2.1.0, KSP 2.1.0-1.0.29) |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |

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
| **Effects** | Live side preview; 6 modes (UV Glow, Pulse, Light Rays, SOS, Strobe, Blacklight); animation speed 0.5×–2.0×; reduced-motion and strobe-safety callouts |
| **Themes** | 5 colour swatches + HSV custom picker; built-in themes; create / rename / apply / update / delete custom themes |
| **Presets** | Save the current look; preview cards; apply, rename and delete; "Active" state |
| **Auto-off timer** | 1 / 5 / 10 / 30 min; live ring countdown; start, pause, resume and cancel; switches the light (and LED) off when it ends |
| **Settings** | Reduced motion; optional phone LED; replay intro; reset settings (with confirm); About & safety |
| **Premium** | Free vs Premium table; monthly and yearly plans; checkout; restore; subscription status; demo preview switch |

Free plan limits: 4 modes (Strobe and Blacklight are Premium), 5 presets, 1 custom theme.

## Architecture

```
com.tubers.blacklight
├── UvGlowApp, MainActivity          app entry, splash, edge-to-edge
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
├── billing/                         BillingGateway + PlayBillingGateway (Google Play Billing 7)
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
  gating, presets, themes, LED sync, timer expiry, power-off cancels timer, reset)

## Hardware: phone LED

*Settings › Phone flashlight (LED)* is optional and off by default. It uses
`CameraManager.setTorchMode`, which needs **no camera permission**. It checks
`FEATURE_CAMERA_FLASH` and `FLASH_INFO_AVAILABLE`, prefers the rear camera, reports errors such as
"camera busy in another app", and turns the LED off when the light turns off, when the timer ends, or
when you leave the app.

## Billing / Premium

By default the Premium screen runs in **clearly labelled demo mode**: "Continue · demo checkout" never
charges and never pretends a purchase succeeded. *Preview Premium (demo)* unlocks features locally for
testing and is shown as "Premium preview (demo)" in the status line.

To go live:

1. Create two **subscriptions** in Play Console (monthly and yearly), each with an active base plan
   and offer.
2. Put their product IDs in `gradle.properties`:
   ```properties
   uvglow.billing.monthlyProductId=uvglow_premium_monthly
   uvglow.billing.yearlyProductId=uvglow_premium_yearly
   ```
3. Upload a signed build to an internal testing track and add licence testers.
4. **Recommended:** verify purchase tokens on your own server (Play Developer API) before granting
   entitlement. The app acknowledges purchases client-side, which is Play's minimum requirement.
5. Hide or remove the *Preview Premium (demo)* switch for release builds (for example with
   `if (BuildConfig.DEBUG)` in `PremiumScreen`).

## Production release checklist

- [ ] Create an upload keystore and add a `signingConfig` for `release` (never commit the keystore).
- [ ] Bump `versionCode` / `versionName` in `app/build.gradle.kts`.
- [ ] Replace launcher icons with a high-resolution export (the included icons are generated from the
      264 px design asset). Android Studio › *New › Image Asset* creates every density.
- [ ] Billing set up as above; remove the demo preview switch from release.
- [ ] Optional: bundle the design's fonts (Sora, JetBrains Mono) under `res/font` and set them in
      `ui/theme/Theme.kt` (the app uses the system sans-serif and monospace today).
- [ ] Play listing: describe the app as a *screen light effect*; do not claim UV output, stain
      detection, banknote checking or disinfection.
- [ ] Data safety form: no data collected or shared; settings stay on the device (Android backup only).
- [ ] `./gradlew bundleRelease` and test the R8-minified build before uploading.

## Known limitations

- The timer lives in the app process. If Android kills the app in the background, the timer stops
  (the light can't be on then anyway). A foreground service would be needed for a timer that only
  controls the LED with the app closed.
- Portrait only.
- Soft beam edges use layered gradients rather than real blur, so they look the same on API 26–35.
