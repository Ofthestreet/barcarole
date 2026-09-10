# Barcarole

*[Version française](README.md)*

A music player for Android that plays the files already sitting on the phone or its SD card.
Nothing else.

## The idea

Somewhere out at sea there is no network. No mobile data, no Wi-Fi, no streaming, no "sign in
to continue", no sync failing at the worst possible moment. What is left is the phone, and
the music on it.

Most players assume the opposite: a connection, an account, a remote catalogue — and they
treat local files as a slightly neglected special case. Barcarole does the reverse. Local
files are the only subject. Everything that assumes a network has been removed, not switched
off.

The name comes from the barcarolle, the song of the Venetian gondoliers: music meant to be
sung on the water.

## What the app cannot do, by construction

- **It has no network access at all.** This is not a setting to untick: the internet
  permission is not declared in the first place. Android physically prevents it from talking
  to anything. It cannot send statistics, check for an update, or fetch cover art.
- **No account, no sign-up, no login.**
- **No cover art, no thumbnails.** That is a choice: dense lists instead, so more tracks fit
  on screen at once.

## Installing

The app is not on the Play Store. It is distributed directly — what Android calls installing
from an "unknown source", with the warnings that come with it, explained right after.

> **[Download the latest release](https://github.com/Ofthestreet/barcarole/releases/latest)**

**Step by step, from the phone:**

1. Open the link above and tap the `barcarole-v0.1.0.apk` file attached to the release. The
   browser downloads it.
2. Open the downloaded file: from the download notification, or from the **Downloads** folder
   in the file manager.
3. Android refuses the first time: "For your security, your phone is not allowed to install
   unknown apps from this source." Tap **Settings**, turn on **Allow from this source**, then
   go back. That permission is granted once, but per source app: the browser and the file
   manager count as two.
4. The install screen appears. Play Protect may offer to scan the app, or report an unknown
   developer — see the next section. Carry on.
5. **Install**, then open.

**To pick up a development build** before it is published: the repository's **Actions** tab,
latest successful run (green tick), download the **`barcarole-debug-apk`** archive and unzip
it to get the APK.

### Updating

Builds are signed with a stable key, held in the repository secrets. A new version **installs
over the previous one** and keeps the settings, the favourites and the listening history. That
is verified rather than assumed: two successive builds produce an identical signing
certificate.

**One exception, once.** Versions from before the rename carried the application id
`com.cdelarue.localmusic`. To Android that is a **different app**: the first
`io.github.ofthestreet.barcarole` build will not replace it, it will install alongside. So
uninstall the old one once, then install the new one. After that, updates follow on without
losing anything.

## "Unverified app": what that actually means

Every Android app is signed; the question is **by whom**. Barcarole is signed with an
anonymous debug key, generated automatically by the build tooling. It attests to nothing: no
identity, no publisher, no review.

Three different warnings can show up, and they do not say the same thing:

| What the phone says | What it means | Can it be removed? |
|---|---|---|
| "Unknown apps from this source" | The file did not come from a store | No — that is what direct installation is. The permission is granted once per source. |
| Play Protect: "scan app", "unknown developer" | Google does not recognise the signature | It can be dismissed. Turning Play Protect off is possible but unwise. |
| "App not installed" when updating | The signature differs from the installed one | ✅ Fixed: builds are signed with a stable key |

**What a stable key fixes.** Generate a key once, keep it, and sign every version with it:
updates then install over the previous one losing nothing, and the app finally has a constant,
verifiable identity. That is the step that matters, and it depends on no store at all.

**What a stable key does not fix.** The "unknown source" warning stays: it is not about the
key, it is about the channel. As long as the file arrives through a browser, Android will
warn.

**The only way to remove the warning entirely** is to go through a recognised store:

- **Google Play** — a one-off $25 developer account, an app review, and the option of staying
  private (internal or closed testing) rather than publishing to the world;
- **F-Droid** — no paid account, but its own requirements (open sources, reproducible builds)
  and a wait.

**Worth watching.** Google has announced a **developer identity verification** requirement for
apps installed outside a store on certified Android devices, rolling out country by country
from 2026, with a lighter regime announced for hobbyist developers. The terms have shifted
several times since the announcement: if this app is to survive that change, check the rules
in force at the time.

## First launch

The app asks for **access to audio files**. That is the only permission it truly needs:
without it there is nothing to play. It also asks to post notifications — optional, and used
only for the playback controls in the notification shade and on the lock screen.

It then indexes what Android already knows about the music on the phone, SD card included.
There is no folder to point it at and no scan to start: it is immediate, and the library
keeps itself up to date as files are added or removed.

## Getting around

Five tabs, one of them optional: **Songs**, *(Albums)*, **Artists**, **Queue**,
**Playlists**. The now-playing bar stays visible at the bottom of every screen; tapping it
opens the full player.

## The features that deserve an explanation

### The playlists fill themselves

There is no playlist to create or maintain. The Playlists tab derives its lists from what you
actually listen to:

- **Favourites** — the ones you marked with a heart.
- **Most played** — this month, this year, or of all time.
- **Recently added** — the last week, the last month, the last 2 or 3 months.
- **Never played** — the bilge: everything you copied across and never listened to.

The periods are chips, and the list below changes with them.

**What counts as a listen:** a track is only counted once half of it has played, or thirty
seconds for a long one — whichever comes first. Skipping through ten tracks therefore does
not inflate the counters, and leaving one on repeat counts every pass.

### The heart lives in the playback bar

Marking a favourite does not mean opening a menu: the heart sits directly in the now-playing
bar, within thumb's reach while the track plays. Favourites are available from the car too.

### Deleting a file

The bin, next to the heart, deletes **the file itself from the phone**, not just its entry in
the library. A confirmation names the track and its artist first. On Android 11 and later the
system shows **its own** confirmation on top of the app's: it owns that right and does not
delegate it, so one deletion asks two questions. And it is final — nothing goes to a
recycle bin.

Once the file is gone the app forgets the rest: the track leaves the queue, and its favourite
status and listening history disappear with it.

### Clearing out duplicates

In the settings, **Remove duplicates** looks for files that are the same thing twice —
typically one track copied in from two different sources.

The match is deliberately strict: same title, same artist and **the same length to the
second**. A live take, a cover, a remaster or another encode that does not land on exactly
the same duration is not treated as a duplicate. That is on purpose: better to miss a
duplicate than to delete a recording you wanted to keep.

Nothing is deleted without passing in front of you. The screen lists every group, shows which
copy is **kept** and which would be **deleted**, with their folder and size, and any group
can be unticked. The copy kept is the largest file, which is usually the better encode. One
confirmation follows, stating how much space it frees.

### Albums are optional

If you listen to tracks rather than records, the Albums tab is noise. A switch in the
settings makes it disappear, along with albums in the search results.

### Folders, and the SD card

The app knows the real path of every file, so it can present them by folder — useful when the
library is organised by hand, and for finding what lives on the SD card rather than in the
phone's own storage. Since it is more of a back door than a daily route, it sits in the
settings rather than in the tabs.

### Ignoring very short files

A slider in the settings keeps anything shorter than N seconds out of the library. Set to
thirty seconds by default, it holds ringtones, notification sounds and voice memos out of the
song list.

### Text size

Four steps, from 100% down to 70%. Shrinking the text shrinks **only** the text: rows, icons
and touch targets keep their size. On a small or tired screen that buys several visible lines
without making the buttons hard to hit. It stacks with Android's own setting: if the phone's
font is already enlarged, this brings it back down.

### Searching, and moving through a long list

Search covers titles, artists, albums and folder names. On long lists, an alphabet rail down
the right edge jumps straight to a letter.

### The queue

It is a tab of its own, not a hidden screen. You can see what is coming, drag to reorder,
swipe a track away, or clear the lot. Any track can be played next or added to the end from a
long press.

## In the car

Barcarole appears in Android Auto with favourites, songs, artists and folders, and answers
voice search.

A sideloaded app does not show up in the car by default: Android Auto only accepts what comes
from a store, unless the lock is lifted. This is a one-off.

Android Auto has no icon to open any more — the "on phone screen" mode was removed in 2022 and
only a system component remains. Its settings are reached through the phone's own Settings:
the surest route is to open *Settings* and search for `Android Auto`, otherwise
*Settings → Connected devices → Connection preferences → Android Auto*. On some phones that
entry only appears after the phone has been connected to a car once.

1. Scroll to the bottom, tap **Version** about ten times and accept the dialog: developer mode
   is unlocked.
2. Menu **⋮** → **Developer settings** → switch on **Unknown sources**.
3. Under **Customise launcher**, check that Barcarole is actually enabled — being listed is not
   always enough, and this is where its place in the car's launcher is chosen.
4. Force stop Android Auto, then reconnect the phone to the car.

Open Barcarole once on the phone and grant it access to audio files before any of this: the car
queries the same service the app does, and without the permission the lists would arrive empty.

## What happens when you leave the app

Playback carries on: it is held by a background service, not by the screen. The controls stay
in the notification shade and on the lock screen, headset and Bluetooth buttons work, playback
pauses when headphones are unplugged, and it ducks and resumes around a phone call.

After a phone restart, the app finds its queue, the current track and the exact position
within it.

## The settings at a glance

| Setting | What it does |
|---|---|
| Theme | Light, dark, or follow the system |
| Dynamic colour | Takes the palette from the wallpaper (Android 12 and later) |
| Text size | 100%, 90%, 80% or 70% |
| Show albums | Shows or hides the Albums tab |
| Browse by folder | Opens the library arranged by folder |
| Remove duplicates | Finds and proposes the spare copies |
| Ignore short tracks | Keeps files under a given length out |
| Rescan | Forces a fresh index of the library |

## Two warnings

**Deletions are final.** The bin and the duplicate cleanup erase files from the phone. There
is no undo and no intermediate recycle bin. Before a large cleanup, try it on a track you do
not care about.

**The app only knows this phone.** It has no idea what exists elsewhere: if a file is nowhere
else and you delete it, it is gone. At sea there will be no backup to download again.

---

To build the app from source, see [docs/BUILD.md](docs/BUILD.md).
