# AGENTS.md

Instructions for coding agents (Claude Code, Codex, Gemini CLI, Cursor and others) helping someone
clean up and speed up their Android TV with this repo. People start with [README.md](README.md);
the prompt they paste is [PROMPT.md](PROMPT.md).

## What this repo does

1. **Debloat** an Android TV / Google TV over ADB, reversibly (`pm disable-user`, never uninstall system apps).
2. **Install a light home screen** (`launcher/`, package `com.cleantv.home`): Apple TV style dock, one
   glass highlight, the focused app's colour glow, a beach + aurora backdrop that follows the time of day,
   optional personal avatar and characters, two languages (Indonesian / English), and an idle screen.
3. **Personalise** it without touching code: `assets/config.json` and `assets/custom/` (see
   [docs/CUSTOMIZE.md](docs/CUSTOMIZE.md)).

## The look belongs to the person

Everything you see in this repo's screenshots is an **example**, not the design to copy: the couple
("Sam & Alex"), the New York skyline in the mockup, the beach in the launcher, the colours and the team
card. The screen is meant to be customised, so ask the person what they want before you draw anything:

- **Their picture**: a photo, an illustration, a cartoon of themselves, a pet, a mascot, or nothing at all.
  Use what they give you; do not pick or generate people for them. Only use pictures of the person
  themselves or of people who agreed to it, and never label a picture with a real public figure's name.
- **The illustration style**, if they want one made (flat, cartoon, line art, photo cut-out) and who or
  what it shows.
- **The place in the backdrop**: a beach, a city skyline, mountains, their home town, an abstract aurora,
  plain black. Whatever they choose still follows the four times of day (sky, sun or moon, lights at night).
- **Names, monogram, colours, info card, dock apps**: their own, or none.

Show their choices on the mockup and change them until they approve. If they have no preference, offer
the examples here as a starting point and say they are only examples.

## The order of work (do not skip ahead)

Follow [docs/WORKFLOW.md](docs/WORKFLOW.md). In short:

1. **Design first, build later.** Agree the look with the person on a mockup (any HTML canvas they can
   see), one decision at a time. Build nothing and change nothing on the TV until they say the design is
   approved.
2. **Connect and back up.** ADB over the network, then save `pm list packages`, the disabled list,
   `getprop` and `settings list global|secure|system` to `backup/<date>/` before the first change.
3. **Write the undo file first** (`tv-undo.txt` next to the backup), then make each change.
4. **Debloat in small batches**, one `pm` command per shell call, screenshot and check Home and Wi-Fi
   between batches ([docs/DEBLOAT.md](docs/DEBLOAT.md)).
5. **Build** the launcher (`launcher/`, JDK 17, Android SDK 34) and **install**; make it Home only when asked.
6. **Verify on the real TV**: screenshot every screen, D-pad from every edge, Back from every view,
   `logcat -b crash`, `dumpsys meminfo com.cleantv.home`.
7. **Close ADB** when done (`settings put global adb_enabled 0`, then check port 5555 refuses).

## Rules that are not negotiable

- **Ask before every change on the TV**, and before any download (name the file, source and size).
- **Disable, never uninstall** system packages. Uninstall only user-installed apps, only when the person asks.
- **Never disable Android's core packages** (permission controller, package installer, ext services/shared,
  module metadata, `com.android.providers.*` except calendar/contacts/user dictionary, `com.android.systemui`,
  `com.android.shell`, `com.android.settings`, `com.android.tv.settings`, `android`), the TV/HDMI/tuner
  input services, the remote and Bluetooth pairing, the on-screen keyboard, Play Store and Play Services.
- **Never launch a factory or test package** (`mediatek.factorymenu.ui`, `com.cvte.tv.fac.*`,
  `com.at.factory.*` and the like), not even to look. Use `dumpsys package <name>` instead.
- **Check setupwraith before disabling it**: `dumpsys wifi | grep -o 'cname=[^ ]*'`. If
  `com.google.android.tungsten.setupwraith` owns a saved network, disabling it deletes that Wi-Fi network
  and the TV drops off Wi-Fi and ADB. Leave it enabled.
- **Disable the stock launcher only after** the new one is installed and confirmed as Home with
  `dumpsys activity activities | grep mResumedActivity`. On some TVs (Sharp/MediaTek Android 11 for one)
  `set-home-activity` and the HOME role succeed but the stock launcher still takes the Home key until it is
  disabled.
- **Verify every download**: the Android command-line tools against the SHA-256 on developer.android.com,
  Gradle against gradle.org's checksum page (and pin it in `gradle-wrapper.properties`), the JDK through an
  official package manager (winget, brew, apt).
- **Keep the launcher light**: no network, no analytics, no libraries beyond Android and the Kotlin stdlib,
  no live blur or video. Paint backdrops once into small bitmaps (see `Backdrop.kt`).
- **No personal data in commits**: no IP addresses, Wi-Fi names, serial numbers, account emails, people's
  names or photos. Personal values go in `assets/config.json` and `assets/custom/`, which the person keeps
  private (`.gitignore` already ignores the images).
- **Someone using the remote during your key-injection tests will scramble the results.** Ask them to put
  it down before you test with `input keyevent`.

## Repo map

| Path | What it is |
|---|---|
| `PROMPT.md` | The prompt a person pastes into their agent. |
| `docs/WORKFLOW.md` | The full procedure: design, assets, toolchain, build, install, verify, close. |
| `docs/DEBLOAT.md` | What is safe to disable, what to keep, and the undo commands. |
| `docs/DESIGN.md` | The approved design as numbers: layout, colours per time of day, focus, glow. |
| `docs/CUSTOMIZE.md` | `config.json` keys and the `custom/` images. |
| `launcher/` | The home screen app (Kotlin, Android framework only, minSdk 26, targetSdk 34). |

## Useful commands

```bash
adb connect <tv ip>:5555
adb -s <tv ip>:5555 shell pm list packages -3          # user-installed apps
adb -s <tv ip>:5555 shell pm disable-user --user 0 <pkg>
adb -s <tv ip>:5555 shell pm enable <pkg>              # undo
adb -s <tv ip>:5555 shell dumpsys diskstats            # storage per app (installs fail below ~5% free)
adb -s <tv ip>:5555 shell pm trim-caches 999G          # clears app caches only, never app data
adb -s <tv ip>:5555 shell screencap -p /sdcard/s.png && adb -s <tv ip>:5555 pull /sdcard/s.png
adb -s <tv ip>:5555 shell settings put secure screensaver_components com.cleantv.home/.AmbientDream
```
