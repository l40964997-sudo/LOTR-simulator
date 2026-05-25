package nl.rug.oop.rts.util;

import lombok.Setter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Procedural and file-based sound effect player plus background music.
 * <p>
 * Ships with a tiny synthesiser that renders short sine-wave cues (battle
 * horn, victory chord, march beat) so the game has audible feedback with
 * no extra assets. Drop a WAV file named after an {@link Effect} (lower
 * case, e.g. {@code battle_horn.wav}) into {@code src/main/resources/sounds/}
 * to replace the procedural beep; {@link #startPlaylist()} cycles through
 * every {@code *.wav} in that folder as background music.
 * <p>
 * Sound output is best effort: when no audio device is available the
 * manager silently drops the request so callers stay free of try/catch noise.
 */
@Setter
public final class SoundManager {

    /** Singleton instance. */
    private static final SoundManager INSTANCE = new SoundManager();

    /** PCM audio format used for the procedural synthesiser. */
    private static final AudioFormat FORMAT =
            new AudioFormat(SoundSynthesizer.SAMPLE_RATE, 16, 1, true, false);

    /** Classpath folder scanned for sound assets. */
    private static final String SOUND_DIR = "sounds";

    /** Per-effect cached PCM byte buffer for the procedural fallback. */
    private final Map<Effect, byte[]> proceduralCache = new EnumMap<>(Effect.class);

    /** Per-effect cached {@link Clip} loaded from a WAV file, when present. */
    private final Map<Effect, Clip> clipCache = new EnumMap<>(Effect.class);

    /** Master switch the user can flip from the toolbar. */
    private boolean enabled = true;

    /** Currently playing looped music clip, or {@code null}. */
    private Clip musicClip;

    /** Last requested music resource name; lets us resume after toggling. */
    private String musicResource;

    /** Linear music volume in [0, 1]; converted to dB before applying. */
    private float musicVolume = 0.7f;

    /** Resolved file names of the active playlist, in play order. */
    private final List<String> playlist = new ArrayList<>();

    /** Index into {@link #playlist} of the currently playing track. */
    private int playlistIndex;

    /** Whether the playlist auto-advances in random order. */
    private boolean shuffle;

    /** Random source for shuffle mode. */
    private final Random shuffleRng = new Random();

    /**
     * Private constructor; use {@link #getInstance()}.
     */
    private SoundManager() {
        for (Effect effect : Effect.values()) {
            proceduralCache.put(effect, SoundSynthesizer.synthesise(effect));
            String path = SOUND_DIR + "/" + effect.name().toLowerCase(Locale.ROOT) + ".wav";
            Clip clip = tryLoadClip(path, false);
            if (clip != null) {
                clipCache.put(effect, clip);
            }
        }
    }

    /**
     * Returns the global sound manager.
     *
     * @return the singleton instance
     */
    public static SoundManager getInstance() {
        return INSTANCE;
    }

    /**
     * Reports whether sound output is currently enabled.
     *
     * @return {@code true} when {@link #play(Effect)} will actually play
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Toggles the master sound switch. Disabling also stops the music
     * loop; re-enabling restarts the playlist if one was running.
     *
     * @param enabled the new state
     */
    public void setEnabled(boolean enabled) {
        boolean hadPlaylist = !playlist.isEmpty();
        this.enabled = enabled;
        if (!enabled) {
            stopMusic();
        } else if (hadPlaylist) {
            playPlaylistTrack();
        } else if (musicResource != null) {
            playSingleLoop(musicResource);
        }
    }

    /**
     * Plays the given effect on a fresh background thread.
     *
     * @param effect the effect to play; {@code null} is silently ignored
     */
    public void play(Effect effect) {
        if (!enabled || effect == null) {
            return;
        }
        Clip clip = clipCache.get(effect);
        if (clip != null) {
            playEffectClip(clip);
            return;
        }
        byte[] pcm = proceduralCache.get(effect);
        if (pcm == null) {
            return;
        }
        Thread t = new Thread(() -> writeToLine(pcm), "sound-" + effect.name().toLowerCase(Locale.ROOT));
        t.setDaemon(true);
        t.start();
    }

    /**
     * Convenience for the old single-track API: forwards to
     * {@link #playSingleLoop(String)} so call sites do not need updating.
     *
     * @param resourceName the file name in the {@code sounds/} folder
     */
    public void playMusic(String resourceName) {
        playSingleLoop(resourceName);
    }

    /**
     * Stops and releases the active music clip, if any.
     * <p>
     * Nulls {@link #musicClip} before calling {@code stop()} so the
     * line-listener used by {@link #startPlaylist()} can tell the
     * difference between "this clip ended naturally" and "we asked it to
     * stop because we're switching tracks".
     */
    public void stopMusic() {
        Clip toStop = musicClip;
        musicClip = null;
        if (toStop == null) {
            return;
        }
        try {
            toStop.stop();
            toStop.close();
        } catch (IllegalStateException ignored) {
            // Already closed; harmless.
        }
    }

    /**
     * Discovers every {@code *.wav} in the {@code sounds/} folder and plays
     * them back-to-back. When the last track ends the playlist wraps to
     * the start so background music never stops.
     */
    public void startPlaylist() {
        stopMusic();
        playlist.clear();
        playlist.addAll(discoverAllWavs());
        if (playlist.isEmpty()) {
            System.err.println("[SoundManager] no .wav files found in sounds/ to play");
            return;
        }
        playlistIndex = 0;
        playPlaylistTrack();
    }

    /**
     * Skips to the next track in the playlist immediately.
     */
    public void nextTrack() {
        if (playlist.isEmpty()) {
            return;
        }
        advanceIndex();
        playPlaylistTrack();
    }

    /**
     * Returns whether the playlist auto-advances in random order.
     *
     * @return {@code true} when shuffling
     */
    public boolean isShuffle() {
        return shuffle;
    }

    /**
     * Toggles shuffle mode for the playlist.
     *
     * @param shuffle the new shuffle setting
     */
    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    /**
     * Returns the name of the track currently playing, if any.
     *
     * @return the active track name, or {@code null} when nothing is playing
     */
    public String getCurrentTrackName() {
        if (playlist.isEmpty() || playlistIndex < 0 || playlistIndex >= playlist.size()) {
            return null;
        }
        return playlist.get(playlistIndex);
    }

    /**
     * Starts (or restarts) one named music track on a continuous loop.
     * Useful when you want a single fixed background piece rather than
     * cycling through a playlist.
     *
     * @param resourceName the file name in the {@code sounds/} folder
     */
    public void playSingleLoop(String resourceName) {
        stopMusic();
        playlist.clear();
        if (resourceName == null || !enabled) {
            return;
        }
        Clip clip = tryLoadClip(SOUND_DIR + "/" + resourceName, true);
        if (clip == null) {
            return;
        }
        applyVolume(clip, musicVolume);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        musicClip = clip;
    }

    /**
     * Picks the next playlist index, accounting for shuffle mode.
     */
    private void advanceIndex() {
        if (shuffle && playlist.size() > 1) {
            int next;
            do {
                next = shuffleRng.nextInt(playlist.size());
            } while (next == playlistIndex);
            playlistIndex = next;
        } else {
            playlistIndex = (playlistIndex + 1) % playlist.size();
        }
    }

    /**
     * Plays the playlist track at {@link #playlistIndex} and arranges for
     * the next one to start as soon as it ends.
     */
    private void playPlaylistTrack() {
        stopMusic();
        if (!enabled || playlist.isEmpty()) {
            return;
        }
        String track = playlist.get(playlistIndex);
        Clip clip = tryLoadClip(SOUND_DIR + "/" + track, true);
        if (clip == null) {
            scheduleAdvance();
            return;
        }
        applyVolume(clip, musicVolume);
        attachAdvanceListener(clip);
        musicClip = clip;
        clip.start();
        System.out.println("[SoundManager] now playing: sounds/" + track
                + " (" + (playlistIndex + 1) + "/" + playlist.size() + ")");
    }

    /**
     * Adds a {@link LineEvent#STOP} listener that auto-advances to the
     * next playlist track when this clip ends naturally.
     *
     * @param clip the clip to monitor
     */
    private void attachAdvanceListener(Clip clip) {
        final Clip self = clip;
        clip.addLineListener(event -> {
            if (event.getType() != LineEvent.Type.STOP) {
                return;
            }
            if (this.musicClip == self) {
                scheduleAdvance();
            }
        });
    }

    /**
     * Advances to the next playlist track on a fresh thread, to avoid
     * recursing inside the line listener's callback thread.
     */
    private void scheduleAdvance() {
        Thread t = new Thread(() -> {
            advanceIndex();
            playPlaylistTrack();
        }, "music-advance");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Lists every {@code *.wav} present in the {@code sounds/} resource
     * folder in alphabetical order. Works when resources are on disk
     * (the IDE / Maven run); returns an empty list inside a packaged JAR.
     *
     * @return the discovered file names, never {@code null}
     */
    private List<String> discoverAllWavs() {
        try {
            URL url = getClass().getClassLoader().getResource(SOUND_DIR);
            if (url == null || !"file".equals(url.getProtocol())) {
                return Collections.emptyList();
            }
            File folder = new File(url.toURI());
            File[] wavs = folder.listFiles(
                    (dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".wav"));
            if (wavs == null || wavs.length == 0) {
                return Collections.emptyList();
            }
            Arrays.sort(wavs);
            List<String> names = new ArrayList<>(wavs.length);
            for (File file : wavs) {
                names.add(file.getName());
            }
            return names;
        } catch (URISyntaxException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the linear music volume in {@code [0, 1]}.
     *
     * @return the current volume
     */
    public float getMusicVolume() {
        return musicVolume;
    }

    /**
     * Sets the linear music volume in {@code [0, 1]} and applies it
     * immediately to any active music clip.
     *
     * @param volume the desired volume; clamped to {@code [0, 1]}
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0f, Math.min(1f, volume));
        if (musicClip != null) {
            applyVolume(musicClip, musicVolume);
        }
    }

    /**
     * Rewinds and triggers a cached effect clip. Re-triggering an already
     * playing clip simply restarts it from the beginning - good enough
     * for short UI cues.
     *
     * @param clip the cached effect clip
     */
    private void playEffectClip(Clip clip) {
        synchronized (clip) {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    /**
     * Attempts to load and open a {@link Clip} from a classpath resource.
     *
     * @param resourcePath the resource path, e.g. {@code "sounds/horn.wav"}
     * @param verbose when {@code true}, failures are logged to stderr
     * @return the opened clip, or {@code null} on any error
     */
    private Clip tryLoadClip(String resourcePath, boolean verbose) {
        URL url = getClass().getClassLoader().getResource(resourcePath);
        if (url == null) {
            if (verbose) {
                System.err.println("[SoundManager] resource not on classpath: " + resourcePath);
            }
            return null;
        }
        try (AudioInputStream raw = AudioSystem.getAudioInputStream(
                new BufferedInputStream(url.openStream()))) {
            AudioInputStream pcm = toPcm(raw);
            Clip clip = AudioSystem.getClip();
            clip.open(pcm);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            if (verbose) {
                System.err.println("[SoundManager] failed to open " + resourcePath
                        + ": " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            }
            return null;
        }
    }

    /**
     * Wraps a non-PCM stream in a converter so {@link Clip} can consume it.
     *
     * @param raw the raw stream straight from {@link AudioSystem}
     * @return a PCM-signed stream usable by Clip
     */
    private AudioInputStream toPcm(AudioInputStream raw) {
        AudioFormat src = raw.getFormat();
        if (src.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
            return raw;
        }
        AudioFormat pcmFmt = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                src.getSampleRate(), 16, src.getChannels(),
                src.getChannels() * 2, src.getSampleRate(), false);
        return AudioSystem.getAudioInputStream(pcmFmt, raw);
    }

    /**
     * Applies a linear volume to a clip via the MASTER_GAIN control.
     *
     * @param clip the target clip
     * @param linear the desired linear volume in [0, 1]
     */
    private void applyVolume(Clip clip, float linear) {
        try {
            FloatControl ctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float gain = (float) (20 * Math.log10(Math.max(0.0001, linear)));
            ctrl.setValue(Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), gain)));
        } catch (IllegalArgumentException ignored) {
            // Control unsupported; leave volume at the system default.
        }
    }

    /**
     * Streams a PCM buffer to the system audio line.
     *
     * @param pcm the PCM data to play
     */
    private void writeToLine(byte[] pcm) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(FORMAT);
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
        } catch (LineUnavailableException ignored) {
            // Audio not available: silently drop the request.
        }
    }

    /**
     * Enumeration of the available sound effects. Override any of these by
     * placing a WAV file named after the constant (lower case) in
     * {@code src/main/resources/sounds/}, e.g. {@code battle_horn.wav}.
     */
    public enum Effect {
        /** A low horn blow used to announce a battle. */
        BATTLE_HORN,
        /** A bright triad announcing victory. */
        VICTORY,
        /** A subdued low tone used after a defeat. */
        DEFEAT,
        /** A short drum loop played during army movement. */
        MARCH,
        /** A crisp click for UI actions. */
        ACTION_CLICK,
        /** A footstep tick for a single army stepping onto an edge. */
        STEP,
        /** A soft ambient drone played on startup. */
        AMBIENT
    }
}
