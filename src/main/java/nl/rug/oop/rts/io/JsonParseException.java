package nl.rug.oop.rts.io;

import java.io.Serializable;

/**
 * Thrown when JSON input does not match the expected grammar.
 *
 * <p>Used by {@link JsonReader} to surface a single, friendly error to
 * the user interface instead of a raw stack trace when a save file is
 * malformed.</p>
 */
public class JsonParseException extends RuntimeException implements Serializable {

    /** Serialisation id. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs the exception with a descriptive message.
     *
     * @param message human readable diagnostic
     */
    public JsonParseException(String message) {
        super(message);
    }
}
