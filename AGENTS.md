## Project Identity
**Name:** Buteyko Breathing
**Type:** Native Android Application
**Language:** Kotlin

## Configuration & Versions
* **AGP:** 8.13.2
* **Kotlin:** 1.9.24
* **KSP:** 1.9.24-1.0.20
* **Compose Compiler:** 1.5.14
* **Java:** 17
* **Hilt:** 2.51.1

## Database Schema (Room)
The database `ButeykoDatabase` consists of three entities:

1.  **`ControlPauseEntity`** (`standalone_cp`)
    * `id`: Long (PK)
    * `timestamp`: Long
    * `durationSeconds`: Float

2.  **`SessionEntity`** (`exercise_sessions`)
    * `id`: Long (PK)
    * `timestamp`: Long
    * `note`: String?

3.  **`ExerciseLoopEntity`** (`exercise_loops`)
    * `id`: Long (PK)
    * `sessionId`: Long (FK -> SessionEntity, CASCADE delete)
    * `timestamp`: Long
    * `initialCp`: Float
    * `breathingDurationSeconds`: Long
    * `finalCp`: Float

## Core Logic & State Machines

### 1. Control Pause (`ControlPauseViewModel`)
* **Timer:** Custom Coroutine loop updating `elapsedSeconds` state.
* **Actions:** Start/Stop/Reset/Delete Last.
* **Logic:** "Delete Last" is only available immediately after recording.
* **Integration:** Can trigger navigation to Exercise (`onStartExercise`), passing the recorded float value to skip the first step.

### 2. Exercise Loop (`ExerciseViewModel`)
* **State Machine:**
    1.  `Idle`
    2.  `PreCheckCp` (Skipped if `initialCp` argument > 0)
    3.  `Breathing` (Wakelock enabled)
    4.  `Paused` (Recovery options)
    5.  `RecoveryCountdown` (30s timer, skippable)
    6.  `PostCheckCp`
    7.  `Summary` (Decision: Next Loop or Finish)
* **Data Flow:** Loops are stored in memory (`completedLoops`) for UI display during the session, but persisted to DB (`ExerciseLoopEntity`) immediately upon completion of each loop.

## Navigation Structure
**Host:** `MainAppShell` (Drawer + Scaffold)
**Routes (`NavRoutes`):**
1.  `control_pause` (Start Destination)
2.  `exercise?initialCp={float}` (Optional arg for jump-start)
3.  `history`
4.  `history_detail/{type}/{id}` (`type` = "CP" | "SESSION")
5.  `charts`

## UI/UX Guidelines
* **Design System:** Material 3.
* **Charts:** Custom `Canvas` implementations (No MPAndroidChart).
* **Formatting:** Time formatted as `mm:ss` or `ss.s`. Dates formatted via `SimpleDateFormat`.
* **Components:** Use `HorizontalDivider` (not `Divider`).