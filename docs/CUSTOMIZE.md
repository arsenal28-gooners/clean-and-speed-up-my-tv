# Customise

Everything personal lives in two places inside `launcher/app/src/main/assets/`. Rebuild and reinstall after
changing them.

## `config.json`

| Key | Example | Effect |
|---|---|---|
| `names` | `"Sam & Alex"` | Added to the greeting ("Good evening, Sam & Alex") and the idle screen. Empty: greeting only. |
| `monogram` | `"S&A"` | Shown in the avatar circle when there is no avatar picture, and on the idle screen. |
| `team` | `"Your FC"` | Shows the next-match card, using `fixtures.json`. Empty: no card. |
| `dock_apps` | package names | The five dock apps until the person picks their own ("Hold OK" on a tile). |

## `fixtures.json`

The team's upcoming matches. Past dates are hidden automatically. `home: true` shows "Home"/"Kandang".

## `custom/`

Your avatar (two poses) and cut-out characters. File names, sizes and tips are in
[`custom/README.md`](../launcher/app/src/main/assets/custom/README.md). These files are ignored by git.

## In the app

- **Language**: Settings pill (top right) > Language: Indonesia / English.
- **Dock**: hold OK on a dock tile to pick another app; hold OK on a tile in "All apps" to put it in the dock.
- **Screensaver**: Settings pill > Screensaver opens Android's screensaver settings; pick "Clean Home ambient".

## Colours and layout

The time-of-day palettes (beach and aurora) are in `Looks.kt`; the layout numbers match
[DESIGN.md](DESIGN.md) and are written in 1920 x 1080 design pixels (`Px.d()` scales them to the screen).
