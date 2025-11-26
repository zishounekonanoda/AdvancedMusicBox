# AdvancedMusicBox

Fork of MusicBox with 1.12–1.21.x support, Japanese/English language toggle, and jukebox fixes.

## Features
- Play `.nbs` songs from commands, discs, jukeboxes, signs, chests, and buttons.
- Range playback: everyone near the jukebox hears the music.
- Playlist via chests, remote via signs/buttons, GUI control panels.
- Language switcher (EN/JA) in the in-game GUI; bundled `lang/ja.yml`.
- Jukebox break fix: drops the inserted disc and stops playback (no ghost discs on re-place).
- Bundled NoteBlockAPI source under `NoteBlockAPI/`.

## Build
```
./gradlew build
```
Output plugin jar: `plugin/build/libs/`.

## Config
- `config.yml` default language is `en`. Use the GUI language button or set `lang: ja` and reload.
- Songs are unpacked to the plugin data folder on first run; add your own `.nbs` under `songs/`.

## Credits
- Original plugin by Spliterash (inspiration / reference).
- Fork maintained at https://github.com/zishounekonanoda/AdvancedMusicBox
