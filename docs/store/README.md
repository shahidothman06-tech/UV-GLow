# UV Glow – Google Play graphics

| File | Size | Use in Play Console |
|---|---|---|
| `../icon-512.png` | 512 × 512 | App icon |
| `feature-graphic.png` | 1024 × 500 | Feature graphic |
| `screenshots/framed/01…06-*.png` | 1080 × 1920 | Phone screenshots (upload these) |
| `screenshots/raw/01…06-*.png` | 1080 × 2400 | Source only: Play rejects images more than twice as tall as wide |

All are 24-bit PNGs (no transparency), well under 8 MB. Store text describes a **screen light
effect**: never call the violet beam real UV / ultraviolet light.

## Regenerate the framed screenshots and feature graphic

```bash
py -3 -m venv .venv
.venv\Scripts\pip install pillow
.venv\Scripts\python docs\store\make_store_images.py
```

The script checks every output (size, PNG, no alpha, < 8 MB, no UV claims) and exits non-zero if
anything fails. Captions and tagline are at the top of the script.

## Recapture the raw screenshots

Use the **release** UI (no Premium crown, no test ads): `gradlew.bat assembleRelease`, then sign a copy
with the local debug key for the emulator only (`zipalign` + `apksigner` with
`%USERPROFILE%\.android\debug.keystore`) and `adb install -r` it. Then, on a 1080 × 2400 emulator:

```bash
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
```

Turn the light **on** (no banner shows while it's on), then capture Home, Effects, Themes, Presets,
Auto-off timer and Settings with `adb exec-out screencap -p > docs/store/screenshots/raw/NN-name.png`.
Finish with `adb shell am broadcast -a com.android.systemui.demo -e command exit`.
