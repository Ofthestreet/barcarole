# Privacy policy — Barcarole

*Last updated: 10 September 2026 · [Version française](confidentialite.md)*

Barcarole is an offline music player for Android. It plays audio files that are already stored
on the phone or its memory card.

## The short version

**Barcarole collects nothing, sends nothing, and has no way of doing either.** It holds no
account, shows no advertising, and contains no analytics or tracking of any kind.

## No network access

This is not a setting that can be switched on later: the application does not declare the
`INTERNET` permission at all. Android therefore blocks any network communication from it at
the operating-system level. It cannot transmit data, check for updates, or load anything from
a server, whoever asked it to.

Anyone can verify this claim without trusting it: the permissions an Android app declares are
listed in its manifest, which is part of the published package, and the source of this one is
[public](https://github.com/Ofthestreet/barcarole).

## What is stored, and where

Everything Barcarole records stays on the device, in the app's own private storage:

- your settings (theme, text size, sort orders, minimum track length);
- which tracks you marked as favourites;
- how many times each track has been played, and when;
- the current playback queue and position.

None of it leaves the phone. None of it is readable by other applications. All of it is
deleted when you uninstall the app — Android removes an app's private storage with the app.

## Permissions, and what each is for

| Permission | Why it is needed |
|---|---|
| Read audio files (`READ_MEDIA_AUDIO`, or `READ_EXTERNAL_STORAGE` on older releases) | To find and play the music already on the device. Without it there is nothing to play. |
| Post notifications | To show the playback controls in the notification shade and on the lock screen. Optional: refusing it only removes those controls. |
| Foreground service, media playback type | To keep playing when the app is in the background or the screen is off. |
| Write external storage (Android 9 and older only) | To delete an audio file when you explicitly ask the app to delete it. |

## Deleting files

Barcarole can delete audio files from the device, but only when you ask it to — through the
bin button, or through the duplicate-removal screen, each of which asks for confirmation
first. On Android 11 and later the operating system asks for its own confirmation as well.
Deletion is permanent: the files do not go to a recycle bin, and the app has no way of
restoring them.

## Children

Barcarole is not directed at children and contains nothing aimed at them. It has no social
features, no user-generated content, and no external links.

## Changes to this policy

Any change to this page is recorded in the repository's history, which is public and
timestamped.

## Contact

Questions about this policy can be raised as an issue on the
[project repository](https://github.com/Ofthestreet/barcarole/issues).
