# Gym Routine Generator PWA

A small offline-first Progressive Web App for generating machine-based gym routines and guiding workouts with tempo/rest cues.

## Run locally

```bash
sbt fastOptJS
python3 -m http.server 4173
```

Then open <http://localhost:4173>.

## Test

```bash
sbt test
```

## Current scope

- Add, edit, and delete workout machines.
- Infer muscle groups and movement pattern from common machine names.
- Manually correct classifications.
- Generate a selected session from a 2-7 day weekly split.
- Include warm-up mobility, easy cardio, and a core finisher in generated routines.
- Adjust generated routine length toward the selected target duration.
- Apply beginner/intermediate/advanced set, rep, tempo, and rest defaults.
- Replace individual generated exercises.
- Follow a workout player with set/rep/tempo/rest state and beep, voice, or silent cues.
- Persist app state in `localStorage`.
- Provide static PWA assets: manifest, service worker, and icon.
