# Brake Bedding

An Android app that coaches you through bedding in new brake pads and rotors, using your
phone's GPS to know when you have hit the speed a stage is asking for.

Bedding lays an even film of pad material onto the rotor face and heat-cycles the pad
compound. Done properly it is why new brakes stop quietly, bite consistently and resist
fade. Skipped, it is why they judder or leave uneven deposits that feel like a warped
rotor. The procedure is not complicated, but it is fiddly to run from memory while
driving — twenty stops from one speed to another, with a set distance between each.

<p align="center">
  <img src="assets/ready.png" alt="Procedure summary with a Start button" width="30%" />
  <img src="assets/hold.png" alt="Hold phase, showing the target speed and a draining countdown bar" width="30%" />
  <img src="assets/brake.png" alt="Brake phase, showing the speed to brake down to" width="30%" />
</p>
<p align="center">
  <img src="assets/cooldown.png" alt="Cooldown phase counting down remaining distance" width="30%" />
  <img src="assets/editor.png" alt="Procedure editor listing stages" width="30%" />
  <img src="assets/guide.png" alt="In-app guide" width="30%" />
</p>

## What it does

- Reads your ground speed from GPS and tells you what to do next
- One full-screen colour per phase, so it is readable at a glance from the driver's seat
- Spoken cues, so you do not have to look at the screen at all
- Every phase also carries a distinct chevron, so nothing depends on colour alone
- A stop ladder across the top showing how much of the procedure is left
- Editable stages, three presets, and mph or km/h

## Safety

**Only run a procedure on a road where it is safe to do so**: straight, level, empty, with
good sight lines and somewhere to abort. You need several miles of continuous driving.

- Stay inside the speed limit and inside your own comfort. If a stage asks for a speed the
  road does not allow, edit the stage down.
- Never come to a standstill during a stop, and do not hold the pedal once you are at the
  target speed. Resting a hot pad against a stationary rotor is what prints an uneven
  deposit into it.
- Stop immediately if you smell burning, the pedal goes long or soft, or the car pulls
  under braking.
- Finish the cooldown before parking.

This app is a timer and a prompt. It cannot see the road. Whatever the pad manufacturer
printed in the box beats whatever is on the screen — the built-in presets are common
community routines, not manufacturer instructions, which is why they are editable.

## Using it

1. Open the app and either accept the default preset or tap **Edit procedure**.
2. For each stage set the number of stops, the speed to start from, the speed to brake
   down to, the distance to cover between stops, and how hard to brake.
3. Keep a cooldown at the end. The app will tell you if there is not one.
4. Find your road, mount the phone, and press **Start**.

While a procedure is running:

| Phase | Means |
|---|---|
| **SPEED UP** (chevrons up) | Get to the stage's start speed |
| **SLOW DOWN** (one chevron down) | You are well over the start speed |
| **HOLD** (two bars) | Sit at that speed while the bar drains |
| **BRAKE** (three chevrons down) | Brake at the stage's intensity, release at the target |
| **COAST** (one bar) | Cover the gap distance so the brakes shed heat |
| **COOL DOWN** (wave) | Drive the cooldown distance with as little braking as you can |

**Pause** freezes the run, **Skip stage** jumps to the next one if the road runs out, and
**Stop** ends it. If GPS drops out the run pauses itself rather than guessing.

## How it works

The interesting part is `engine/BeddingEngine.kt`. It is a pure function — no Android
imports, no timers, no callbacks — that takes a state and an event and returns the next
state. A whole procedure is `events.fold(initial, engine::reduce)`, which means a
thirty-stop run can be tested in microseconds without a device.

```
LocationSpeedSource ──▶ RunViewModel ──▶ BeddingEngine ──▶ RunState ──▶ RunScreen
   (GPS fixes)          (4 Hz ticks,      (pure reducer)                 (Compose)
                         staleness)
```

Two consequences worth calling out:

- **The three-second speed hold is a countdown decremented by each tick's own delta**,
  not a scheduled callback. There is no concurrency in the run loop at all, so overlapping
  timers are not a bug that was fixed but a state that cannot be represented.
- **Procedures are stored in SI units** and converted only at the UI boundary, so
  switching between mph and km/h converts what you see without touching the routine.

Stages are a `@Serializable sealed interface` with one generated polymorphic serializer
used for both reading and writing, so the stored form and the parsed form cannot disagree.

## Building

Requires JDK 17 or newer. Everything else is fetched by the Gradle wrapper.

```bash
git clone https://github.com/nicglazkov/BrakeBeddingApp.git
cd BrakeBeddingApp
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit tests
```

Minimum Android 8.0 (API 26). Compiled against API 37, targets API 36.

### Release signing

Release builds are signed from a `keystore.properties` at the repository root, which is
gitignored and never committed:

```properties
storeFile=/absolute/path/to/your.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Without that file the project still configures and builds; only the release variant comes
out unsigned. That is what CI does, which keeps the build honest about not needing any
maintainer-only material.

## Privacy

The app has no internet permission. Your location is read from the device's GPS, used to
drive the state machine, and never stored or transmitted.

## Licence

Apache 2.0 — see [LICENSE](LICENSE).

## Disclaimer

Provided as-is, without warranty. You are responsible for your own safety and for
following your brake manufacturer's recommendations.
