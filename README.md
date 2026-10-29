# Food Journal — Android

The Android companion to **Mess Ledger**, a private meal journal.

Record breakfast, lunch and dinner; track a monthly missed-meal allowance
and what the food costs over the billing cycle; go back and correct any
day you logged wrong.

This is a native port of the desktop app, built with **Jetpack Compose**.

---

## What it does

| | |
| --- | --- |
| **Today** | three meal rows, allowance, the day's cost |
| **Log** | revisit any past date and fix it |
| **Month** | a month grid with per-meal tick marks |
| **Settings** | prices, tax, allowance, reminders |
| **Home-screen widget** | log a meal without opening the app |
| **Import / Export** | back up your history to CSV, restore it after a reinstall or on a new phone |
| **Sync** | reconciles with your laptop when it is reachable |

Two themes, one journal: **Sunlit Scrapbook** by day, **The Night Ledger**
after dark — warm ink, parchment text and a muted copper accent.

Data is stored locally in a Room database. There is no account, no
telemetry and no third-party SDK.

---

## Install

Grab the APK from the [Releases page](../../releases) and install it.
(You will need to allow installs from your browser — see *Security* below.)

Requires **Android 8.0 (API 26)** or newer.

### If Android blocks the install

Installing an APK you didn't get from the Play Store means enabling
sideloading once:

**Android 9–** — Settings → Security → *Unknown sources* → allow your
browser.

**Android 10+** — the prompt appears when you tap the APK. Choose
*Settings* → *Allow from this source*.

Nothing is uploaded anywhere. The warning exists because an APK can do
anything, which is also true of the Play Store.

---

## Backups, reinstalls and new phones

**Updating the app does not touch your data.** v1.1 and later share the
same application id, so installing a newer APK over an older one keeps
the Room database exactly as it was. Do **not** uninstall first if you
can avoid it.

If you do uninstall — or move to a new phone — take a backup first:

1. **Settings → Export CSV**, and save the file somewhere you will find
   it (Downloads, Drive, a laptop).
2. Install the new version.
3. **Settings → Import CSV** and pick that file.

Import merges. A date and meal that already exists is replaced by the
file; everything else is left alone. The older export only contains the
cycle that was on screen when you pressed export, so export from a
current build when you can.

The CSV is plain text. Desktop exports (lower-case headers,
`True`/`False` cells) import too.

---

## Build it yourself

Requires Android Studio (Ladybug or newer) and JDK 17+.

```bash
git clone https://github.com/FulllOfHope/food-journal-mobile.git
cd food-journal-mobile
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

From the command line on Linux, point Gradle at Android Studio's bundled
JDK if `java` isn't on your PATH:

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew assembleDebug
```

Run the tests with `./gradlew test`.

---

## The rules

Deliberately small, and deliberately the same as the desktop app:

| | |
| --- | --- |
| breakfast | 0.5 |
| lunch | 1 (2 on Saturday) |
| Saturday dinner | closed |
| allowance | 25 missed meals per cycle |

The billing cycle runs from the **3rd** of a month to the **2nd** of the
next, not the calendar month. Misses are counted in meal credits, so
skipping breakfast alone costs half a miss; billing still uses whole
portions, because that is what gets charged.

---

## How it's put together

```text
com.messledger.app
├── MainActivity.kt
├── data/          Room database, DataStore config
├── domain/        BillingEngine — the rules above, in one place
├── export/        CSV import and export
├── sync/          foreground sync with the laptop
├── ui/
│   ├── screens/   Today, Log, Month, Settings
│   ├── components/ shared pieces
│   └── navigation/ NavHost
├── viewmodel/     MessViewModel
└── widget/        Glance home-screen widget
```

Stack: Kotlin 2.0.21 · Jetpack Compose (BOM-managed) · Material 3 · Room
· DataStore · Glance · AGP 9.4.1 · minSdk 26 · targetSdk 37.

**The rules live in `domain/BillingEngine.kt` and nowhere else.** If you
change a portion weight or the cycle length, change it there — the UI
reads from it rather than repeating the numbers.

---

## Sync

The app keeps a copy locally and talks to your laptop when it can be
reached on the same network. If the laptop is off, taps are queued and
sent later — nothing is dropped.

Your laptop runs the matching server from the [desktop
repo](https://github.com/FulllOfHope/food-journal); point the app at
your machine's address in Settings.

---

## Design

Warm, paper-like, hand-drawn. Marks are ticks and tallies rather than
progress bars, with a single accent colour — a stamp red — and no green
anywhere. Progress is shown as a fruiting tree and small marginalia
rather than as chrome.

---

## Licence

Yours. This is a personal project, not a published product.

Built with AndroidX libraries (Apache 2.0).