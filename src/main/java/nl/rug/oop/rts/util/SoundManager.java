package nl.rug.oop.rts.util;

import lombok.Setter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tiny procedural sound effect player.
 * <p>
 * Rather than ship a folder full of {@code .wav} assets, the manager
 * synthesises short PCM clips on the fly using the {@code javax.sound.sampled}
 * API. Each {@link Effect} corresponds to a short, recognisable cue (a
 * battle horn, a victory fanfare, marching footsteps, ...). The clips are
 * generated once and cached as raw byte arrays so playback is essentially
 * free after the first call.
 * <p>
 * Sound output is best-effort: when no audio device is available (CI, a
 * headless laptop, a tester muting their machine) the manager silently
 * drops the request. That keeps the simulator usable on machines without
 * audio without sprinkling try/catch blocks across the caller.
 */
@Setter
public final class SoundManager {

    /** Singleton instance. */
    private static final SoundManager INSTANCE = new SoundManager();

    /** Output sample rate. CD quality is overkill for beeps but easy. */
    private static final float SAMPLE_RATE = 44_100f;

    /** PCM audio format: 16-bit signed, little-endian, mono. */
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    /** Per-effect cached PCM byte buffer. */
    private final Map<Effect, byte[]> cache = new EnumMap<>(Effect.class);

    /** Master switch the user can flip from the toolbar. */
    private boolean enabled = true;

    /**
     * Private constructor; use {@link #getInstance()}.
     */
    private SoundManager() {
        // Pre-generate every effect so the first playback is not delayed.
        for (Effect effect : Effect.values()) {
            cache.put(effect, synthesise(effect));
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
     * Plays the given effect on a fresh background thread.
     *
     * @param effect the effect to play; {@code null} is silently ignored
     */
    public void play(Effect effect) {
        if (!enabled || effect == null) {
            return;
        }
        byte[] pcm = cache.get(effect);
        if (pcm == null) {
            return;
        }
        Thread t = new Thread(() -> writeToLine(pcm), "sound-" + effect.name().toLowerCase());
        t.setDaemon(true);
        t.start();
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
     * Synthesises an effect into raw little-endian 16-bit PCM bytes.
     *
     * @param effect the effect to render
     * @return the rendered PCM data
     */
    private byte[] synthesise(Effect effect) {
        switch (effect) {
            case BATTLE_HORN: return tone(0.70, 220.0, 0.45);
            case VICTORY: return chord(0.90, new double[] {523, 659, 784}, 0.40);
            case DEFEAT: return tone(0.80, 110.0, 0.20);
            case MARCH: return drumPattern(0.50);
            case ACTION_CLICK: return tone(0.10, 880.0, 0.35);
            case STEP: return tone(0.05, 1320.0, 0.25);
            case AMBIENT: return tone(1.20, 165.0, 0.10);
            default: return new byte[0];
        }
    }

    /**
     * Renders a steady sine wave with an exponential envelope.
     *
     * @param seconds duration in seconds
     * @param frequency tone frequency in Hz
     * @param volume peak amplitude in [0, 1]
     * @return the PCM bytes
     */
    private byte[] tone(double seconds, double frequency, double volume) {
        int samples = (int) (SAMPLE_RATE * seconds);
        byte[] out = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double t = i / SAMPLE_RATE;
            double envelope = Math.exp(-2.0 * t / seconds);
            double sample = Math.sin(2 * Math.PI * frequency * t) * volume * envelope;
            writeSample(out, i, sample);
        }
        return out;
    }

    /**
     * Mixes a small chord made of independent sine partials.
     *
     * @param seconds duration in seconds
     * @param frequencies the partial frequencies in Hz
     * @param volume peak amplitude in [0, 1]
     * @return the PCM bytes
     */
    private byte[] chord(double seconds, double[] frequencies, double volume) {
        int samples = (int) (SAMPLE_RATE * seconds);
        byte[] out = new byte[samples * 2];
        double partVolume = volume / Math.max(1, frequencies.length);
        for (int i = 0; i < samples; i++) {
            double t = i / SAMPLE_RATE;
            double envelope = Math.exp(-1.5 * t / seconds);
            double sample = 0.0;
            for (double f : frequencies) {
                sample += Math.sin(2 * Math.PI * f * t) * partVolume * envelope;
            }
            writeSample(out, i, sample);
        }
        return out;
    }

    /**
     * Renders a short repeating low thump to evoke marching feet.
     *
     * @param volume peak amplitude in [0, 1]
     * @return the PCM bytes
     */
    private byte[] drumPattern(double volume) {
        double seconds = 0.6;
        int samples = (int) (SAMPLE_RATE * seconds);
        byte[] out = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double t = i / SAMPLE_RATE;
            double beat = ((int) (t * 4)) % 2 == 0 ? 1.0 : 0.0;
            double envelope = Math.exp(-12.0 * (t - Math.floor(t * 4) / 4));
            double sample = Math.sin(2 * Math.PI * 90.0 * t) * volume * beat * envelope;
            writeSample(out, i, sample);
        }
        return out;
    }

    /**
     * Encodes a normalised sample (-1..1) into a little-endian 16-bit slot.
     *
     * @param out destination buffer
     * @param sampleIndex the sample index (the byte offset is {@code 2*sampleIndex})
     * @param sample the normalised sample value
     */
    private void writeSample(byte[] out, int sampleIndex, double sample) {
        int s = (int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample * Short.MAX_VALUE));
        out[sampleIndex * 2] = (byte) (s & 0xFF);
        out[sampleIndex * 2 + 1] = (byte) ((s >> 8) & 0xFF);
    }

    /**
     * Enumeration of the available sound effects.
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
