# Debloat

Disable with `pm disable-user --user 0 <package>`, one package per shell call; undo with `pm enable <package>`.
Write the undo line to `tv-undo.txt` before you run the disable line.

## Usually safe to disable

| Package | What it is |
|---|---|
| `com.google.android.tvrecommendations` | Recommendation rows for the stock launcher |
| `com.google.android.partnersetup` | Play partner customisations |
| `com.google.android.backdrop` | Google's ambient screensaver (~120 MB); set another screensaver first |
| `com.google.android.videos` | Google TV / Play Movies store |
| `com.google.android.feedback`, `com.android.tv.feedbackconsent` | Feedback and its consent dialog |
| `com.google.android.syncadapters.calendar` | Calendar sync, not used on a TV |
| `android.autoinstalls.config.*` | Partner auto-install stubs |
| `com.mediatek.android.leanbacklauncher.partnercustomizer` | Partner rows injected into the stock launcher |
| `fusion.android.tv.demo`, `*.storedemoapplication` | Retail demo mode |

## Ask first (the household may use them)

Streaming apps (Prime Video, YouTube Music, ...), Google Assistant (`com.google.android.katniss`),
Chromecast (`com.google.android.apps.mediashell`), the phone remote (`com.google.android.tv.remote.service`),
firmware updaters. Put them in one checklist question and disable only what the person does not tick.

## Never disable

The TV app and inputs (`com.mediatek.wwtv.tvcenter`, `com.mediatek.tvinput`, `com.mediatek.tv.service`
on MediaTek sets), the channel setup wizard, the remote and Bluetooth pairing, the keyboard, Play Store and
Play Services, and Android's core packages listed in [AGENTS.md](../AGENTS.md).

`com.google.android.tungsten.setupwraith`: only after `dumpsys wifi | grep -o 'cname=[^ ]*'` shows it does
**not** own a saved network.

The stock launcher (`com.google.android.tvlauncher` or `com.google.android.apps.tv.launcherx`): only after the
new launcher is installed and confirmed as Home.

## Tested on

Sharp AQUOS 4T-C50DK1L (device `banqiao`, MediaTek, Android TV 11, 1.8 GB RAM): everything in the first
table, plus the stock launcher after the new one was Home. setupwraith owned the saved Wi-Fi and was kept.
Storage was 99% full (4 GB): `pm trim-caches` freed ~350 MB of caches before the launcher would install.

Thanks to [cleanyourtv](https://github.com/Gathin23/cleanyourtv) for its safety rules and device notes,
which this guide follows.
