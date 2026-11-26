package parser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Production implementation of LlmClient utilizing standard Java 11+ HttpClient.
 * Works with Gemini, OpenAI, or compatible endpoints via LLM_API_KEY environment variable.
 */
public class HttpLlmClient implements LlmClient {

    private final String apiKey;
    private final HttpClient httpClient;

    public HttpLlmClient(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key must not be null or empty");
        }
        this.apiKey = apiKey.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String chat(String prompt) throws Exception {
        // Support both Gemini and OpenAI compatible endpoints based on key format or environment
        if (apiKey.startsWith("AIza")) {
            return callGemini(prompt);
        } else {
            return callOpenAiCompatible(prompt);
        }
    }

    private String callGemini(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        String requestBody = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [{\"text\": \"" + SimpleJson.escape(prompt) + "\"}]\n" +
                "  }]\n" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("LLM API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        // Extract candidates[0].content.parts[0].text
        String body = response.body();
        int textIdx = body.indexOf("\"text\":");
        if (textIdx != -1) {
            int startQuote = body.indexOf('"', textIdx + 7);
            if (startQuote != -1) {
                return extractJsonString(body, startQuote);
            }
        }
        return body;
    }

    private String callOpenAiCompatible(String prompt) throws Exception {
        String url = System.getenv("LLM_BASE_URL");
        if (url == null || url.trim().isEmpty()) {
            url = "https://api.openai.com/v1/chat/completions";
        }

        String requestBody = "{\n" +
                "  \"model\": \"gpt-4o-mini\",\n" +
                "  \"messages\": [{\"role\": \"user\", \"content\": \"" + SimpleJson.escape(prompt) + "\"}],\n" +
                "  \"temperature\": 0.1\n" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("LLM API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        String body = response.body();
        int contentIdx = body.indexOf("\"content\":");
        if (contentIdx != -1) {
            int startQuote = body.indexOf('"', contentIdx + 10);
            if (startQuote != -1) {
                return extractJsonString(body, startQuote);
            }
        }
        return body;
    }

    private String extractJsonString(String s, int startQuote) {
        StringBuilder sb = new StringBuilder();
        int i = startQuote + 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') {
                break;
            }
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"'); i += 2; break;
                    case '\\': sb.append('\\'); i += 2; break;
                    case 'n':  sb.append('\n'); i += 2; break;
                    case 'r':  sb.append('\r'); i += 2; break;
                    case 't':  sb.append('\t'); i += 2; break;
                    default:   sb.append(next); i += 2; break;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
