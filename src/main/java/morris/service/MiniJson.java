package morris.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A tiny JSON parser (objects, arrays, strings, numbers, booleans, null) for the save format. */
final class MiniJson {
    private final String src;
    private int pos;

    private MiniJson(String src) {
        this.src = src;
    }

    /** Returns Map, List, String, Double, Boolean or null. */
    static Object parse(String text) throws GameFormatException {
        MiniJson p = new MiniJson(text);
        p.skipWs();
        Object value = p.value();
        p.skipWs();
        if (p.pos != p.src.length()) {
            throw p.error("unexpected trailing content");
        }
        return value;
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private Object value() throws GameFormatException {
        if (pos >= src.length()) throw error("unexpected end of input");
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't', 'f', 'n' -> literal();
            default -> number();
        };
    }

    private Map<String, Object> object() throws GameFormatException {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // {
        skipWs();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipWs();
            if (peek() != '"') throw error("expected a string key");
            String key = string();
            skipWs();
            expect(':');
            skipWs();
            map.put(key, value());
            skipWs();
            if (peek() == ',') { pos++; continue; }
            expect('}');
            return map;
        }
    }

    private List<Object> array() throws GameFormatException {
        List<Object> list = new ArrayList<>();
        pos++; // [
        skipWs();
        if (peek() == ']') { pos++; return list; }
        while (true) {
            skipWs();
            list.add(value());
            skipWs();
            if (peek() == ',') { pos++; continue; }
            expect(']');
            return list;
        }
    }

    private String string() throws GameFormatException {
        StringBuilder sb = new StringBuilder();
        pos++; // opening quote
        while (pos < src.length()) {
            char c = src.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                if (pos >= src.length()) break;
                char e = src.charAt(pos++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > src.length()) throw error("bad unicode escape");
                        sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        throw error("unterminated string");
    }

    private Object literal() throws GameFormatException {
        if (src.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
        if (src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        if (src.startsWith("null", pos)) { pos += 4; return null; }
        throw error("unexpected token");
    }

    private Double number() throws GameFormatException {
        int start = pos;
        while (pos < src.length() && "+-0123456789.eE".indexOf(src.charAt(pos)) >= 0) pos++;
        if (start == pos) throw error("unexpected character '" + src.charAt(pos) + "'");
        try {
            return Double.parseDouble(src.substring(start, pos));
        } catch (NumberFormatException e) {
            throw error("bad number");
        }
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private void expect(char c) throws GameFormatException {
        if (peek() != c) throw error("expected '" + c + "'");
        pos++;
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private GameFormatException error(String msg) {
        return new GameFormatException("Invalid JSON at position " + pos + ": " + msg);
    }
}
