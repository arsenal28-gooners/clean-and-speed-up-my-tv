# Design

The approved design, in 1920 x 1080 design pixels. Black ground, Apple TV style, with a backdrop that
follows the time of day.

## Time of day

| Phase | Hours | Greeting (id / en) |
|---|---|---|
| pagi | before 11:00 | Selamat pagi / Good morning |
| siang | 11:00-14:59 | Selamat siang / Good afternoon |
| sore | 15:00-17:59 | Selamat sore / Good afternoon |
| malam | 18:00 and later | Selamat malam / Good evening |

The greeting rotates every 8 s through the phase greeting, Hi, Halo/Hello, Welcome back, Apa kabar/How are you.
The avatar shows the waving pose for Hi, Halo/Hello and Welcome back (200 ms crossfade).

## Backdrop ("kombinasi")

- **Beach band**, top 700 px: sky gradient, sun or moon at x 1180, sea from y 500, sand from y ~590.
  It fades out from 62% of its height to the bottom, and towards the left behind the clock.
- **Aurora** under everything else, for the whole scrolling page (2400 px), one continuous bitmap:
  base colour at the top, black under the dock, base again behind the grid. Ribbons stay out of the dock
  band (728-960) so the only light behind a tile is the focused app's glow.

| Phase | Sky top / low | Sun | Sea | Sand | Aurora base, hue A, hue B |
|---|---|---|---|---|---|
| pagi | #6FA8DC / #FBD3B0 | #FFE3A3 | #5D9CC0 | #EED3A6 | #0B1424, #FFB38A, #7CC4FF |
| siang | #2F8FE0 / #9FD6F5 | #FFF7DC | #1E7FB8 | #F1DCAE | #061626, #3FD0FF, #2BD9A8 |
| sore | #4B3F7A / #F28C5A | #FFB067 | #5B4A78 | #D6A272 | #120A1E, #FF8A4C, #9B6BFF |
| malam | #070C1F / #1C2A55 | #E9ECF5 (moon, stars) | #101C3D | #34364E | #070A14, #2EE6C5, #7B5CFF |

Both layers are painted once per phase into small bitmaps (beach at half size, aurora at quarter size)
and stretched; nothing is blurred or redrawn per frame.

## Layout

| Element | Position / size | Notes |
|---|---|---|
| Greeting + avatar | x 80, y 64; avatar 52 px circle, coral ring | text 32, #D1D1D6 |
| Clock | under the greeting | 168 px, light weight |
| Date | under the clock | 34 px, #AEAEB2 |
| Pills | top right, 60 px tall, 14 px apart | Inputs, Wi-Fi, Settings (round); frosted white 10% |
| Characters | x 1200, y 96, 600 x 596 | bottom fades out (58% to 98%) |
| Info card | x 80, y 420, 500 wide | frosted, team's next match |
| Focused app name | x 80, y 612 | 76 px light, glow in the app's colour |
| Dock band | x 72, y 728, 1776 x 232, radius 44 | 5 banners 326 x 184, 24 apart |
| All apps grid | x 96, y 996, 6 columns, 150 px tall tiles | dimmed to 45% on Home; page scrolls up 560 px on Down |

## Focus

- Dock: one glass highlight (white 16%, 2 px white 32% rim) that springs to the focused tile
  (320 ms, overshoot 1.4); the tile scales to 1.06 and shows its full colour.
- Unfocused tiles are muted: dock saturation 75% / brightness 80%, grid 70% / 75%.
- Grid: white 4 px ring, scale 1.06, label turns white.
- Glow: a 900 x 420 radial glow in the app's colour (alpha ~42%) glides behind the focused tile.
- Back: closes a dropdown, or returns from "All apps" to the first dock tile. Home never closes.

## Dropdowns

Under the pills, dark glass (#1C1C1E at 94%), 28 px radius, 64 px rows:
Inputs (TV antenna + every HDMI/AV the TV reports), Wi-Fi (status, IP, Wi-Fi settings),
Settings (language, screensaver, all Android settings).

## Idle screen

Beach and characters on the left fading to black; on the right the monogram, names, a 300 px light clock,
the date and the next match.
