package nl.rug.oop.rts.util;

import lombok.Getter;
import lombok.Setter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Looping background music player backed by a WAV playlist.
 * <p>
 * Drop one or more {@code .wav} files into {@code src/main/resources/sounds/}
 * and {@link #startPlaylist()} cycles through them back-to-back. Tracks can
 * be skipped with {@link #nextTrack()} and shuffled with
 * {@link #setShuffle(boolean)}. The previous procedural sound-effect engine
 * has been retired; this class now focuses on music only.
 * <p>
 * Audio output is best effort: when no device is available the manager
 * silently drops the request so callers stay free of try/catch noise.
 */
@Getter
@Setter
public final class SoundManager {

    /** Singleton instance. */
    private static final SoundManager INSTANCE = new SoundManager();

    /** Classpath folder scanned for sound assets. */
    private static final String SOUND_DIR = "sounds";

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
        // Music playback only - no procedural effects to pre-render.
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
     * Convenience for the old single-track API: forwards to
     * {@link #playSingleLoop(String)} so existing call sites still work.
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
     * difference between "ended naturally" and "we asked it to stop".
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
     *
     * @param resourceName the file name in the {@code sounds/} folder
     */
    public void playSingleLoop(String resourceName) {
        stopMusic();
        playlist.clear();
        if (resourceName == null) {
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
        if (playlist.isEmpty()) {
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
}
