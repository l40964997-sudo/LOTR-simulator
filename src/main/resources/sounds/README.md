# Sound assets

`SoundManager` looks for audio files in this folder at startup. Anything you
drop here is picked up automatically when the JAR is rebuilt.

## Sound effects (optional overrides)

The game ships with procedurally generated sine-wave cues for every
`SoundManager.Effect`. To replace any of them with a real recording, drop a
WAV file named after the effect (lower case) into this folder:

| Effect            | File name           |
|-------------------|---------------------|
| `BATTLE_HORN`     | `battle_horn.wav`   |
| `VICTORY`         | `victory.wav`       |
| `DEFEAT`          | `defeat.wav`        |
| `MARCH`           | `march.wav`         |
| `ACTION_CLICK`    | `action_click.wav`  |
| `STEP`            | `step.wav`          |
| `AMBIENT`         | `ambient.wav`       |

If a file is missing, the procedural fallback is used. No code change required.

## Background music (looping)

`MainFrame` calls `SoundManager.getInstance().playMusic("theme.wav")` on
startup. Drop a `theme.wav` in here and you have looping background music.
You can switch to a different track at any time by calling
`playMusic("other_file.wav")`. Call `stopMusic()` to silence it.

## Supported formats

JavaSound's built-in providers handle:
- `.wav` (PCM signed 16-bit recommended, mono or stereo) — always works
- `.aiff`, `.au` — usually work
- `.ogg`, `.mp3` — only if an SPI is on the classpath (not by default)

If you have an MP3, the easiest path is to convert it with
[Audacity](https://www.audacityteam.org/) (File -> Export -> WAV) or with
ffmpeg: `ffmpeg -i track.mp3 track.wav`.

## Where to get music legally

Do **not** commit copyrighted soundtracks (Howard Shore's LotR / Hobbit
scores included) - shipping them in a repository is copyright infringement.
The following sources offer LotR-adjacent fantasy music under permissive
licences:

- [Pixabay Music](https://pixabay.com/music/) - CC0, search "epic", "celtic", "fantasy"
- [OpenGameArt.org](https://opengameart.org/) - CC0 / CC-BY game music
- [Incompetech](https://incompetech.com/) (Kevin MacLeod) - CC-BY; try
  "Crusade", "Bard's Tale", "Hidden Wonders"
- [Free Music Archive](https://freemusicarchive.org/) - filter by Creative Commons
- [Freesound.org](https://freesound.org/) - sound effects (war horns, sword
  clashes, horse gallop, crowd cheers)

When you use a CC-BY track, add the required attribution to your `README.md`
or an in-game credits screen.
