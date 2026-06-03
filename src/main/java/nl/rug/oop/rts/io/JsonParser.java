package nl.rug.oop.rts.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny recursive-descent JSON parser used by {@link JsonReader}.
 * <p>
 * Accepts the JSON subset the assignment needs: objects, arrays, strings,
 * numbers, booleans, and {@code null}. No external library is used, in line
 * with the spec. Errors during parsing are reported as
 * {@link JsonParseException} so the UI can surface a single friendly
 * dialog rather than a stack trace.
 * <p>
 * Splitting the parser out keeps {@link JsonReader} focused on rebuilding
 * the graph from the parsed structure and lets the parsing primitives be
 * reused or unit-tested in isolation.
 */
public class JsonParser {

    /** Source string being parsed. */
    private String source = "";

    /** Current cursor position. */
    private int pos;

    /**
     * Parses a JSON document into the corresponding Java structure.
     * Returns a {@link Map}, {@link List}, {@link String}, {@link Number},
     * {@link Boolean}, or {@code null}.
     *
     * @param text the document
     * @return the parsed root value
     */
    public Object parse(String text) {
        this.source = text == null ? "" : text;
        this.pos = 0;
        skipWhitespace();
        return readValue();
    }

    /**
     * Reads any JSON value at the cursor.
     *
     * @return the parsed value: a map, list, string, number, boolean or null
     */
    private Object readValue() {
        skipWhitespace();
        if (pos >= source.length()) {
            throw new JsonParseException("Unexpected end of input");
        }
        char c = source.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            default -> readScalar(c);
        };
    }

    /**
     * Reads a scalar value (number, boolean or null) at the cursor.
     *
     * @param c the lookahead character
     * @return the parsed scalar
     */
    private Object readScalar(char c) {
        if (c == '-' || (c >= '0' && c <= '9')) {
            return readNumber();
        }
        if (matchKeyword("true")) {
            return Boolean.TRUE;
        }
        if (matchKeyword("false")) {
            return Boolean.FALSE;
        }
        if (matchKeyword("null")) {
            return null;
        }
        throw new JsonParseException("Unexpected character '" + c + "' at position " + pos);
    }

    /**
     * Reads a JSON object at the cursor.
     *
     * @return a map of the object's keys to values
     */
    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> map = new HashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        boolean more = true;
        while (more) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            map.put(key, readValue());
            skipWhitespace();
            more = consumeSeparator('}');
        }
        return map;
    }

    /**
     * Reads a JSON array at the cursor.
     *
     * @return a list of the array's values
     */
    private List<Object> readArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        boolean more = true;
        while (more) {
            list.add(readValue());
            skipWhitespace();
            more = consumeSeparator(']');
        }
        return list;
    }

    /**
     * Consumes a separator after a collection element: a comma to continue
     * or the closing bracket to stop.
     *
     * @param closing the closing bracket character expected at the end
     * @return {@code true} if more elements follow, {@code false} if closed
     */
    private boolean consumeSeparator(char closing) {
        char c = peek();
        if (c == ',') {
            pos++;
            return true;
        }
        if (c == closing) {
            pos++;
            return false;
        }
        throw new JsonParseException("Expected ',' or '" + closing + "' at position " + pos);
    }

    /**
     * Reads a quoted JSON string at the cursor, resolving escape sequences.
     *
     * @return the unescaped string content
     */
    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < source.length()) {
            char c = source.charAt(pos);
            pos++;
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                appendEscape(sb);
            } else {
                sb.append(c);
            }
        }
        throw new JsonParseException("Unterminated string");
    }

    /**
     * Reads and appends one escape sequence to the builder.
     *
     * @param sb the builder receiving the decoded character
     */
    private void appendEscape(StringBuilder sb) {
        if (pos >= source.length()) {
            throw new JsonParseException("Dangling backslash");
        }
        char esc = source.charAt(pos);
        pos++;
        switch (esc) {
            case '"' -> sb.append('"');
            case '\\' -> sb.append('\\');
            case '/' -> sb.append('/');
            case 'b' -> sb.append('\b');
            case 'f' -> sb.append('\f');
            case 'n' -> sb.append('\n');
            case 'r' -> sb.append('\r');
            case 't' -> sb.append('\t');
            case 'u' -> appendUnicodeEscape(sb);
            default -> throw new JsonParseException("Bad escape");
        }
    }

    /**
     * Reads a four hex digit unicode escape and appends the character.
     *
     * @param sb the builder receiving the decoded character
     */
    private void appendUnicodeEscape(StringBuilder sb) {
        if (pos + 4 > source.length()) {
            throw new JsonParseException("Bad unicode escape");
        }
        int codePoint = Integer.parseInt(source.substring(pos, pos + 4), 16);
        sb.append((char) codePoint);
        pos += 4;
    }

    /**
     * Reads a JSON number at the cursor.
     *
     * @return a {@link Long} for integers or a {@link Double} otherwise
     */
    private Number readNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < source.length() && isNumberChar(source.charAt(pos))) {
            pos++;
        }
        String token = source.substring(start, pos);
        return parseNumberToken(token);
    }

    /**
     * Reports whether a character can appear inside a JSON number token.
     *
     * @param c the character to test
     * @return {@code true} if it is a digit, sign, dot or exponent marker
     */
    private boolean isNumberChar(char c) {
        return Character.isDigit(c) || c == '.' || c == 'e' || c == 'E'
                || c == '+' || c == '-';
    }

    /**
     * Parses a numeric token into a boxed number.
     *
     * @param token the textual number
     * @return the parsed value
     */
    private Number parseNumberToken(String token) {
        try {
            if (token.indexOf('.') >= 0 || token.indexOf('e') >= 0 || token.indexOf('E') >= 0) {
                return Double.valueOf(token);
            }
            return Long.valueOf(token);
        } catch (NumberFormatException ex) {
            throw new JsonParseException("Bad number '" + token + "'");
        }
    }

    /**
     * Attempts to match a literal keyword at the cursor, advancing on match.
     *
     * @param keyword the keyword to match
     * @return {@code true} if matched and consumed
     */
    private boolean matchKeyword(String keyword) {
        if (pos + keyword.length() > source.length()) {
            return false;
        }
        if (source.regionMatches(pos, keyword, 0, keyword.length())) {
            pos += keyword.length();
            return true;
        }
        return false;
    }

    /**
     * Consumes the expected character or throws.
     *
     * @param c the required character
     */
    private void expect(char c) {
        if (pos >= source.length() || source.charAt(pos) != c) {
            throw new JsonParseException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }

    /**
     * Returns the character at the cursor without consuming it.
     *
     * @return the lookahead character, or NUL at end of input
     */
    private char peek() {
        if (pos >= source.length()) {
            return '\0';
        }
        return source.charAt(pos);
    }

    /**
     * Advances the cursor past any whitespace.
     */
    private void skipWhitespace() {
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
    }
}
