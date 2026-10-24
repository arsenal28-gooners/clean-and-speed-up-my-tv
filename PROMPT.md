# The prompt

Paste this into Claude Code (or another coding agent with shell access) on a computer on the same
Wi-Fi as the TV. Mention that you cloned this repo, so the agent reuses `launcher/` instead of writing
a new one.

Before you start, on the TV: Settings > Device Preferences > About > press OK on "Build" 7 times, then
Developer options > turn on USB debugging (and Network/ADB debugging if it is there).

---

I want to clean up and speed up my Android TV and give it a calm, personal home screen.
My TV model is: <MODEL>. Its IP address is: <TV IP>.

Follow AGENTS.md and docs/WORKFLOW.md from this repo, step by step, on the real TV.

- Start with the design. Show me a mockup of the home screen first and ask me one design question at a
  time (colours, what the top bar shows, which apps go in the dock, whether I want an avatar or
  characters, light/time-of-day backdrop). Do not build or change the TV until I say "design approved".
- Ask me for my images when the design needs them: an avatar (normal and waving) and a cut-out picture of
  us or a mascot. If I only have a normal photo or illustration, help me make the cut-out and the small
  WebP files described in launcher/app/src/main/assets/custom/README.md.
- Put my names, monogram and team in launcher/app/src/main/assets/config.json. Keep them out of any
  public commit.
- Before every change on the TV, tell me the exact command and wait for my yes. Write the undo commands
  to a file before you run them.
- Before downloading anything (JDK, Android SDK, Gradle), tell me what, from where and how big, and
  verify the checksums.
- When everything works, close network ADB and check the port is closed.

---

Expect a few questions: which apps you use (some can be disabled), the dock apps and their order,
whether to keep the voice assistant and Chromecast, and the look. Everything is reversible.
