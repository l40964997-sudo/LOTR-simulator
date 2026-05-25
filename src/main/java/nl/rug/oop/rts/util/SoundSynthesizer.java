package nl.rug.oop.rts.util;

/**
 * Tiny in-memory sound synthesiser used as a fallback when no WAV file is
 * provided for a {@link SoundManager.Effect}.
 * <p>
 * The class renders short PCM (16-bit signed, mono, 44.1 kHz) byte buffers
 * for sine tones, chords and a simple drum pattern. Keeping these helpers
 * separate from {@link SoundManager} keeps the manager focused on playback
 * orchestration and makes the file lengths comfortable for the
 * Checkstyle configuration.
 */
final class SoundSynthesizer {

    /** Output sample rate. Must match the format expected by the player. */
    static final float SAMPLE_RATE = 44_100f;

    /**
     * Utility class; not instantiable.
     */
    private SoundSynthesizer() {
        throw new AssertionError("Utility class");
    }

    /**
     * Synthesises an effect into raw little-endian 16-bit PCM bytes.
     *
     * @param effect the effect to render
     * @return the rendered PCM data
     */
    static byte[] synthesise(SoundManager.Effect effect) {
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
    static byte[] tone(double seconds, double frequency, double volume) {
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
    static byte[] chord(double seconds, double[] frequencies, double volume) {
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
    static byte[] drumPattern(double volume) {
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
     * @param sampleIndex the sample index (byte offset is {@code 2*sampleIndex})
     * @param sample the normalised sample value
     */
    private static void writeSample(byte[] out, int sampleIndex, double sample) {
        int s = (int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample * Short.MAX_VALUE));
        out[sampleIndex * 2] = (byte) (s & 0xFF);
        out[sampleIndex * 2 + 1] = (byte) ((s >> 8) & 0xFF);
    }
}
