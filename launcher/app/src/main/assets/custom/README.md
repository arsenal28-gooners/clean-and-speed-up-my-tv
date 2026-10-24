# Your images

Put your own pictures in this folder. All three are optional; anything missing is simply left out
(the avatar falls back to the monogram from `config.json`).

| File name (`.webp`, `.png` or `.jpg`) | What it is | Suggested size |
|---|---|---|
| `avatar_normal` | Round avatar next to the greeting, normal pose | 256 x 256, square |
| `avatar_wave` | Same avatar, waving pose (shown for "Hi", "Halo", "Welcome back") | 256 x 256, square |
| `characters` | Cut-out people or mascot on a **transparent** background, shown over the beach (top right on Home, bottom left on the idle screen) | about 650 x 650, transparent PNG or WebP |

Tips

- Keep each file small (WebP quality ~80: avatars 10-40 KB, characters 30-60 KB). The TV has little RAM.
- `characters` needs a transparent background. If you only have a picture on a plain dark background,
  ask your AI agent to cut it out (see `docs/WORKFLOW.md`, "Assets").
- These files are ignored by git (`.gitignore`), so your photos never end up in the public repo.
