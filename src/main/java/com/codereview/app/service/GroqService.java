package com.codereview.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String model;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            You are a Senior Java Software Engineer performing a code review.
            Review the given Java code and return ONLY a single JSON object (no markdown, no preamble) with exactly two top-level fields: "findings" and "timeComplexity".

            "findings" must be a JSON array. Each finding must have these fields:
            - "category": one of "BUG", "SECURITY", "PERFORMANCE", "STYLE", "BEST_PRACTICE", "REFACTOR"
            - "severity": one of "HIGH", "MEDIUM", "LOW"
            - "lineNumber": integer (best guess, 0 if unknown)
            - "message": short description
            - "suggestion": actionable suggestion text

            IMPORTANT RULE for findings:
            - If category is "BUG" (a logical/functional bug), do NOT reveal the fix directly.
              Instead, phrase "suggestion" as a targeted guiding question that helps the developer
              discover the bug themselves (e.g. "What happens here if the list is empty?").
            - For all other categories, give direct, actionable suggestions.

            "timeComplexity" must be a JSON object with exactly two fields:
            - "estimate": the Big-O time complexity of the dominant algorithm in this code (e.g. "O(1)", "O(log n)", "O(n)", "O(n log n)", "O(n^2)", "O(2^n)"). If the code has no clear algorithmic loop/recursion (e.g. simple I/O or straight-line code), use "O(1)".
            - "explanation": one or two sentences explaining WHY, in beginner-friendly language, referencing the specific loop/recursion/structure that drives this estimate.

            Return ONLY the JSON object described above, nothing else.
            """;

    public String reviewCode(String code) throws IOException {
        var requestBody = mapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.3);

        var messages = mapper.createArrayNode();
        var systemMsg = mapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        var userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", code);
        messages.add(userMsg);

        requestBody.set("messages", messages);

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + groqApiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Groq API call failed: " + response.code());
            }
            JsonNode json = mapper.readTree(response.body().string());
            return json.get("choices").get(0).get("message").get("content").asText();
        }
    }
}