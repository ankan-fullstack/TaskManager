package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight JSON parser and serializer written in plain Java with zero external dependencies.
 * Handles object parsing, string unescaping, markdown fence stripping, and JSON formatting.
 */
public final class SimpleJson {

    private SimpleJson() {
    }

    /**
     * Cleans an LLM response by stripping markdown code blocks (e.g. ```json ... ```)
     * and trimming leading/trailing whitespace.
     */
    public static String cleanJson(String raw) {
        if (raw == null) return "";
        String s = raw.trim();

        // Strip markdown fences
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline != -1) {
                s = s.substring(firstNewline + 1);
            } else {
                s = s.replaceFirst("^```[a-zA-Z]*", "");
            }
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    /**
     * Parses a flat or simple JSON object into a Map of key-value pairs.
     * String values are returned unescaped and without quotes.
     * Null literals or omitted values are represented as null.
     */
    public static Map<String, String> parseObject(String jsonStr) {
        if (jsonStr == null) return Collections.emptyMap();
        String json = cleanJson(jsonStr);

        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start == -1 || end == -1 || start >= end) {
            return Collections.emptyMap();
        }

        String content = json.substring(start + 1, end).trim();
        Map<String, String> map = new LinkedHashMap<>();
        int len = content.length();
        int i = 0;

        while (i < len) {
            // Find key starting quote
            while (i < len && Character.isWhitespace(content.charAt(i))) i++;
            if (i >= len) break;

            if (content.charAt(i) != '"') {
                // Unexpected character, advance
                i++;
                continue;
            }

            // Parse key
            String key = parseJsonString(content, i);
            i += escapedLength(content, i);

            // Find colon
            while (i < len && content.charAt(i) != ':') i++;
            if (i >= len) break;
            i++; // skip ':'

            // Skip whitespace after colon
            while (i < len && Character.isWhitespace(content.charAt(i))) i++;
            if (i >= len) break;

            // Parse value
            char c = content.charAt(i);
            String value;
            if (c == '"') {
                value = parseJsonString(content, i);
                i += escapedLength(content, i);
            } else if (c == '{' || c == '[') {
                // Nested object or array - extract matching bracket slice
                int valueEnd = findMatchingBracket(content, i, c);
                value = content.substring(i, valueEnd + 1);
                i = valueEnd + 1;
            } else {
                // Literal (number, boolean, null)
                int startVal = i;
                while (i < len && content.charAt(i) != ',' && content.charAt(i) != '}' && content.charAt(i) != ']') {
                    i++;
                }
                value = content.substring(startVal, i).trim();
                if ("null".equalsIgnoreCase(value)) {
                    value = null;
                }
            }

            map.put(key, value);

            // Skip until next comma or end
            while (i < len && content.charAt(i) != ',') i++;
            if (i < len && content.charAt(i) == ',') i++;
        }

        return map;
    }

    /**
     * Extracts a single string value by key from a JSON object string.
     */
    public static String getString(String json, String key) {
        Map<String, String> parsed = parseObject(json);
        return parsed.get(key);
    }

    /**
     * Parses an array of JSON objects: [ {"a": "1"}, {"a": "2"} ]
     */
    public static List<Map<String, String>> parseArrayOfObjects(String jsonStr) {
        if (jsonStr == null) return Collections.emptyList();
        String json = cleanJson(jsonStr);

        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start == -1 || end == -1 || start >= end) {
            return Collections.emptyList();
        }

        List<Map<String, String>> result = new ArrayList<>();
        int i = start + 1;
        while (i < end) {
            while (i < end && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= end) break;
            if (json.charAt(i) == '{') {
                int objEnd = findMatchingBracket(json, i, '{');
                String objStr = json.substring(i, objEnd + 1);
                result.add(parseObject(objStr));
                i = objEnd + 1;
            } else {
                i++;
            }
        }
        return result;
    }

    private static String parseJsonString(String s, int startIndex) {
        StringBuilder sb = new StringBuilder();
        int i = startIndex + 1; // skip opening quote
        int len = s.length();

        while (i < len) {
            char c = s.charAt(i);
            if (c == '"') {
                break;
            }
            if (c == '\\' && i + 1 < len) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"'); i += 2; break;
                    case '\\': sb.append('\\'); i += 2; break;
                    case '/':  sb.append('/'); i += 2; break;
                    case 'b':  sb.append('\b'); i += 2; break;
                    case 'f':  sb.append('\f'); i += 2; break;
                    case 'n':  sb.append('\n'); i += 2; break;
                    case 'r':  sb.append('\r'); i += 2; break;
                    case 't':  sb.append('\t'); i += 2; break;
                    case 'u':
                        if (i + 5 < len) {
                            String hex = s.substring(i + 2, i + 6);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException ignored) {
                            }
                            i += 6;
                        } else {
                            i += 2;
                        }
                        break;
                    default:
                        sb.append(next);
                        i += 2;
                        break;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static int escapedLength(String s, int startIndex) {
        int i = startIndex + 1;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == '"') {
                return (i - startIndex) + 1;
            }
            if (c == '\\' && i + 1 < len) {
                i += 2;
            } else {
                i++;
            }
        }
        return len - startIndex;
    }

    private static int findMatchingBracket(String s, int startIndex, char openBracket) {
        char closeBracket = (openBracket == '{') ? '}' : ']';
        int depth = 0;
        boolean inQuotes = false;
        int len = s.length();

        for (int i = startIndex; i < len; i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == openBracket) {
                    depth++;
                } else if (c == closeBracket) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return len - 1;
    }

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
