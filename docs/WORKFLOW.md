# Workflow

The procedure an agent follows with a person, in order. Each stage ends with something the person
approves or can see.

## 1. Design on a mockup (no TV changes yet)

1. Build an HTML mockup of the home screen at 1920 x 1080 (any canvas the person can open in a browser).
   Start from [DESIGN.md](DESIGN.md); it is the approved reference design.
2. Ask design questions **one at a time**, with 2-3 options and a recommendation each. The screen is
   customisable: the person decides which photo or illustration is on it and which place the backdrop
   shows (see "The look belongs to the person" in [AGENTS.md](../AGENTS.md)). For example:
   - their picture: their own photo, an illustration or cartoon of them (which style), a pet or mascot, or none;
   - the place: a beach, a city skyline (the public mockup shows New York), mountains, their home town,
     or only the aurora; it follows the time of day either way;
   - background layout: the scene alone, aurora alone, or a combination (scene band on top, aurora
     everywhere else);
   - identity: names in the greeting, a monogram, an avatar with two poses, characters over the backdrop;
   - quick settings: dropdowns under the top-right pills (Inputs, Wi-Fi, Settings);
   - the info card (next match of a team, or none);
   - dock apps and order (5 by default), and whether "All apps" sits below the dock.
3. Show all four times of day side by side (morning, midday, afternoon, night) so the person can judge
   colours without waiting for the clock.
4. Check contrast as you go: text at 4.5:1 or more; large text over a bright sky needs a darker band or a
   fade behind it.
5. Stop and wait for "design approved". No build, no app images and no TV command before that, and any
   later change to the look is shown on the mockup first.

## 2. Assets

Ask the person for their images only once the design needs them (an AI image tool is fine for this).

- **Avatar**: two square pictures, normal and waving, 256 x 256 WebP at quality ~80 (10-40 KB each).
- **Characters**: a cut-out on a transparent background, about 650 x 650 WebP (30-60 KB).
  If the person has a picture on a plain dark background, cut it out by flood-filling the background from
  the edges and stopping at the dark outline of the drawing (an outline is darker than both sides; compare
  each pixel with the mean of its 9 x 9 neighbourhood). Keep the largest connected shape. Show the cut-out
  on a magenta background to check the edges before using it.
- A browser can encode WebP without installing anything: draw the image on a `<canvas>`, then
  `canvas.toBlob(cb, 'image/webp', 0.8)`, served from a tiny local server that saves the result.

Put the files in `launcher/app/src/main/assets/custom/` (see the README there).

## 3. Connect and back up

```bash
adb connect <tv ip>:5555          # accept "Allow USB debugging?" on the TV, tick "Always allow"
adb -s <tv ip>:5555 shell getprop ro.build.version.sdk     # must be 26 or higher
```

Save to `backup/<date>/`: `pm list packages`, `pm list packages -d`, `getprop`,
`settings list global`, `settings list secure`, `settings list system`. Never commit this folder: it holds
the serial number.

If the TV answers `unauthorized` later, it is waiting for the dialog again: reconnect and ask the person
to accept it.

## 4. Debloat

See [DEBLOAT.md](DEBLOAT.md). Write `tv-undo.txt` first, one batch at a time, screenshot and check Home and
Wi-Fi after each batch.

## 5. Toolchain (ask before downloading)

| Tool | From | Check |
|---|---|---|
| JDK 17 | `winget install EclipseAdoptium.Temurin.17.JDK` (or brew/apt) | package manager hash |
| Android command-line tools | dl.google.com | SHA-256 listed on developer.android.com/studio |
| platforms;android-34, build-tools;34.0.0 | `sdkmanager` | sdkmanager checksums |
| Gradle 8.9 | services.gradle.org | SHA-256 on gradle.org/release-checksums, pinned in the wrapper |

`sdkmanager --licenses` needs the person's agreement to the Android SDK licence. On Windows, feed the
answers with `cmd /c "sdkmanager --licenses < yes.txt"`; a PowerShell pipe does not reach it.

Create `launcher/local.properties` with `sdk.dir=<path to the SDK>`.

## 6. Build and install

```bash
cd launcher
./gradlew assembleRelease            # app/build/outputs/apk/release/app-release.apk, ~120 KB
adb -s <tv ip>:5555 install -r app/build/outputs/apk/release/app-release.apk
```

- `INSTALL_FAILED_INSUFFICIENT_STORAGE` / "not enough space": check `dumpsys diskstats`; with the person's
  yes, `pm trim-caches 999G` clears caches only. Installs need roughly 5% of the storage free.
- Make it Home only when asked: `cmd package set-home-activity com.cleantv.home/.MainActivity`, then
  `cmd role add-role-holder android.app.role.HOME com.cleantv.home`. If Home still opens the stock
  launcher, disable the stock launcher (with the person's yes) and check again.
- Screensaver: `settings put secure screensaver_components com.cleantv.home/.AmbientDream`.

## 7. Verify on the TV

- Screenshot Home, each dock tile, "All apps" (every row), each dropdown, Back from every view.
- `adb logcat -b crash` must stay empty; `dumpsys meminfo com.cleantv.home` (about 55 MB PSS on a 1080p TV).
- Ask the person not to use the remote while you inject keys.
- Fix what you see, rebuild, reinstall, look again.

## 8. Close

```bash
adb -s <tv ip>:5555 shell settings put global adb_enabled 0
```

Then check that TCP port 5555 refuses connections. Tell the person how to turn debugging back on, and
where the undo file is.
