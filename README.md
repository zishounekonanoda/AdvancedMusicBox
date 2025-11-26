# AdvancedMusicBox

Fork of MusicBox with 1.21.x support, Japanese/English language toggle, and jukebox fixes.
# english

## Features
- Play `.nbs` songs from commands, discs, jukeboxes, signs, chests, and buttons.
- Range playback: everyone near the jukebox hears the music.
- Playlist via chests, remote via signs/buttons, GUI control panels.
- Language switcher (EN/JA) in the in-game GUI; bundled `lang/ja.yml`.
- Jukebox break fix: drops the inserted disc and stops playback (no ghost discs on re-place).
- Bundled NoteBlockAPI source under `NoteBlockAPI/`.

## Config
- `config.yml` default language is `en`. Use the GUI language button or set `lang: ja` and reload.
- Songs are unpacked to the plugin data folder on first run; add your own `.nbs` under `songs/`.

## Credits
nekorin

# 日本語
AdvancedMusicBox は、Note Block Studio（NBS）形式の音楽を Minecraft 内で自然に再生できるようにするためのプラグインです。GUI 操作、カスタムディスクを使用してジュークボックスでの再生、
看板・チェスト・ボタンによるプレイリストや再生管理など、専門的な知識がなくても扱えるよう設計されています。サーバー全体での英語・日本語切り替えにも対応しており、幅広いユーザー環境で使用できます。
