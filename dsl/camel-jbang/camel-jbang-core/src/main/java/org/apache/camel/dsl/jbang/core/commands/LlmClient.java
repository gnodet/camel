/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.dsl.jbang.core.commands;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.camel.dsl.jbang.core.common.CommandLineHelper;
import org.apache.camel.dsl.jbang.core.common.Printer;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;

/**
 * Shared LLM HTTP client supporting Ollama, OpenAI-compatible, and Anthropic (including Vertex AI) APIs.
 */
class LlmClient {

    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    private static final String DEFAULT_ANTHROPIC_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String VERTEX_ANTHROPIC_VERSION = "vertex-2023-10-16";
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-6";
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 5;

    enum ApiType {
        ollama,
        openai,
        anthropic
    }

    // -- Unified abstractions for tool-calling across API formats --

    record ToolDef(String name, String description, JsonObject parameters) {
    }

    record ToolCall(String id, String name, JsonObject arguments) {
    }

    record ToolResult(String toolCallId, String content) {
    }

    record Message(String role, String content, List<ToolCall> toolCalls, List<ToolResult> toolResults) {

        static Message user(String text) {
            return new Message("user", text, null, null);
        }

        static Message assistantWithToolCalls(String text, List<ToolCall> calls) {
            return new Message("assistant", text, calls, null);
        }

        static Message toolResults(List<ToolResult> results) {
            return new Message("tool", null, null, results);
        }
    }

    record ChatResponse(String text, List<ToolCall> toolCalls, String stopReason) {
    }

    // -- Configuration --

    ApiType apiType;
    String url;
    String apiKey;
    String model;
    int timeout;
    double temperature;
    boolean stream;
    int maxTokens;
    Printer printer;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build();

    // Vertex AI specific
    private String vertexRegion;
    private String vertexProjectId;

    // -- Builder --

    static LlmClient create() {
        return new LlmClient();
    }

    LlmClient withApiType(ApiType apiType) {
        this.apiType = apiType;
        return this;
    }

    LlmClient withUrl(String url) {
        this.url = url;
        return this;
    }

    LlmClient withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    LlmClient withModel(String model) {
        this.model = model;
        return this;
    }

    LlmClient withTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    LlmClient withTemperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    LlmClient withStream(boolean stream) {
        this.stream = stream;
        return this;
    }

    LlmClient withMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    LlmClient withPrinter(Printer printer) {
        this.printer = printer;
        return this;
    }

    // -- Auto-detection --

    boolean detectEndpoint() {
        if (tryExplicitUrl()) {
            return true;
        }
        if (apiType != null) {
            return switch (apiType) {
                case anthropic -> tryAnthropicOrVertex();
                case openai -> tryOpenAi();
                case ollama -> tryInfraOllama() || tryDefaultOllama();
            };
        }
        // auto-detect priority: anthropic → vertex → openai → ollama
        return tryAnthropicApiKey()
                || tryVertexAi()
                || tryOpenAi()
                || tryInfraOllama()
                || tryDefaultOllama();
    }

    String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }
        if (apiType == ApiType.anthropic) {
            String key = System.getenv("ANTHROPIC_API_KEY");
            if (key != null && !key.isBlank()) {
                apiKey = key;
                return key;
            }
            // Vertex AI uses gcloud token, not API key
            return null;
        }
        return Stream.of("OPENAI_API_KEY", "LLM_API_KEY")
                .map(System::getenv)
                .filter(k -> k != null && !k.isBlank())
                .findFirst()
                .map(k -> {
                    apiKey = k;
                    return k;
                })
                .orElse(null);
    }

    // -- Simple generate (for explain) --

    String generate(String systemPrompt, String userPrompt) {
        return switch (apiType) {
            case ollama -> generateOllama(systemPrompt, userPrompt);
            case openai -> generateOpenAi(systemPrompt, userPrompt);
            case anthropic -> generateAnthropic(systemPrompt, userPrompt);
        };
    }

    // -- Chat with tools (for ask) --

    ChatResponse chatWithTools(String systemPrompt, List<Message> messages, List<ToolDef> tools) {
        return switch (apiType) {
            case ollama, openai -> chatOpenAiFormat(systemPrompt, messages, tools);
            case anthropic -> chatAnthropicFormat(systemPrompt, messages, tools);
        };
    }

    // ---- Ollama generate ----

    private String generateOllama(String systemPrompt, String userPrompt) {
        JsonObject request = new JsonObject();
        request.put("model", model);
        request.put("prompt", userPrompt);
        request.put("system", systemPrompt);
        request.put("stream", stream);

        JsonObject options = new JsonObject();
        options.put("temperature", temperature);
        request.put("options", options);

        if (stream) {
            return sendStreamingRequest(url + "/api/generate", request, null, "response");
        }
        JsonObject response = sendRequest(url + "/api/generate", request, null);
        return response != null ? response.getString("response") : null;
    }

    // ---- OpenAI-compatible generate ----

    private String generateOpenAi(String systemPrompt, String userPrompt) {
        JsonArray messages = new JsonArray();
        messages.add(createOpenAiMessage("system", systemPrompt));
        messages.add(createOpenAiMessage("user", userPrompt));

        JsonObject request = new JsonObject();
        request.put("model", model);
        request.put("messages", messages);
        request.put("temperature", temperature);

        String resolvedKey = resolveApiKey();
        String apiUrl = normalizeOpenAiUrl(url);

        JsonObject response = sendRequest(apiUrl, request, resolvedKey);
        return extractOpenAiContent(response);
    }

    // ---- Anthropic generate ----

    private String generateAnthropic(String systemPrompt, String userPrompt) {
        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.put("role", "user");
        JsonArray content = new JsonArray();
        JsonObject textBlock = new JsonObject();
        textBlock.put("type", "text");
        textBlock.put("text", userPrompt);
        content.add(textBlock);
        userMsg.put("content", content);
        messages.add(userMsg);

        JsonObject request = buildAnthropicRequest(systemPrompt, messages, null);

        String apiUrl = resolveAnthropicUrl();
        Map<String, String> headers = buildAnthropicHeaders();

        if (stream) {
            return sendAnthropicStreamingRequest(apiUrl, request, headers);
        }
        JsonObject response = sendRequestWithHeaders(apiUrl, request, headers);
        return extractAnthropicTextContent(response);
    }

    // ---- OpenAI/Ollama chat with tools ----

    private ChatResponse chatOpenAiFormat(String systemPrompt, List<Message> messages, List<ToolDef> tools) {
        JsonArray jsonMessages = new JsonArray();
        jsonMessages.add(createOpenAiMessage("system", systemPrompt));

        for (Message msg : messages) {
            if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                // assistant message with tool calls
                JsonObject assistantMsg = new JsonObject();
                assistantMsg.put("role", "assistant");
                if (msg.content() != null) {
                    assistantMsg.put("content", msg.content());
                }
                JsonArray toolCalls = new JsonArray();
                for (ToolCall tc : msg.toolCalls()) {
                    JsonObject call = new JsonObject();
                    call.put("id", tc.id());
                    call.put("type", "function");
                    JsonObject function = new JsonObject();
                    function.put("name", tc.name());
                    function.put("arguments", tc.arguments().toJson());
                    call.put("function", function);
                    toolCalls.add(call);
                }
                assistantMsg.put("tool_calls", toolCalls);
                jsonMessages.add(assistantMsg);
            } else if (msg.toolResults() != null && !msg.toolResults().isEmpty()) {
                // tool result messages (one per result for OpenAI)
                for (ToolResult tr : msg.toolResults()) {
                    JsonObject toolMsg = new JsonObject();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", tr.toolCallId());
                    toolMsg.put("content", tr.content());
                    jsonMessages.add(toolMsg);
                }
            } else {
                jsonMessages.add(createOpenAiMessage(msg.role(), msg.content()));
            }
        }

        JsonObject request = new JsonObject();
        request.put("model", model);
        request.put("messages", jsonMessages);
        request.put("temperature", temperature);

        if (tools != null && !tools.isEmpty()) {
            JsonArray jsonTools = new JsonArray();
            for (ToolDef tool : tools) {
                JsonObject toolObj = new JsonObject();
                toolObj.put("type", "function");
                JsonObject function = new JsonObject();
                function.put("name", tool.name());
                function.put("description", tool.description());
                function.put("parameters", tool.parameters());
                toolObj.put("function", function);
                jsonTools.add(toolObj);
            }
            request.put("tools", jsonTools);
        }

        String apiUrl = apiType == ApiType.ollama
                ? url + "/api/chat"
                : normalizeOpenAiUrl(url);
        String resolvedKey = resolveApiKey();

        JsonObject response = sendRequest(apiUrl, request, resolvedKey);
        return parseOpenAiChatResponse(response);
    }

    // ---- Anthropic chat with tools ----

    private ChatResponse chatAnthropicFormat(String systemPrompt, List<Message> messages, List<ToolDef> tools) {
        JsonArray jsonMessages = new JsonArray();

        for (Message msg : messages) {
            if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                // assistant message with tool_use blocks
                JsonObject assistantMsg = new JsonObject();
                assistantMsg.put("role", "assistant");
                JsonArray content = new JsonArray();
                if (msg.content() != null) {
                    JsonObject textBlock = new JsonObject();
                    textBlock.put("type", "text");
                    textBlock.put("text", msg.content());
                    content.add(textBlock);
                }
                for (ToolCall tc : msg.toolCalls()) {
                    JsonObject toolUse = new JsonObject();
                    toolUse.put("type", "tool_use");
                    toolUse.put("id", tc.id());
                    toolUse.put("name", tc.name());
                    toolUse.put("input", tc.arguments());
                    content.add(toolUse);
                }
                assistantMsg.put("content", content);
                jsonMessages.add(assistantMsg);
            } else if (msg.toolResults() != null && !msg.toolResults().isEmpty()) {
                // tool results as user message with tool_result blocks
                JsonObject userMsg = new JsonObject();
                userMsg.put("role", "user");
                JsonArray content = new JsonArray();
                for (ToolResult tr : msg.toolResults()) {
                    JsonObject toolResult = new JsonObject();
                    toolResult.put("type", "tool_result");
                    toolResult.put("tool_use_id", tr.toolCallId());
                    toolResult.put("content", tr.content());
                    content.add(toolResult);
                }
                userMsg.put("content", content);
                jsonMessages.add(userMsg);
            } else {
                JsonObject m = new JsonObject();
                m.put("role", msg.role());
                JsonArray content = new JsonArray();
                JsonObject textBlock = new JsonObject();
                textBlock.put("type", "text");
                textBlock.put("text", msg.content());
                content.add(textBlock);
                m.put("content", content);
                jsonMessages.add(m);
            }
        }

        JsonArray jsonTools = null;
        if (tools != null && !tools.isEmpty()) {
            jsonTools = new JsonArray();
            for (ToolDef tool : tools) {
                JsonObject toolObj = new JsonObject();
                toolObj.put("name", tool.name());
                toolObj.put("description", tool.description());
                toolObj.put("input_schema", tool.parameters());
                jsonTools.add(toolObj);
            }
        }

        JsonObject request = buildAnthropicRequest(systemPrompt, jsonMessages, jsonTools);
        String apiUrl = resolveAnthropicUrl();
        Map<String, String> headers = buildAnthropicHeaders();

        JsonObject response = sendRequestWithHeaders(apiUrl, request, headers);
        return parseAnthropicChatResponse(response);
    }

    // ---- Anthropic helpers ----

    private JsonObject buildAnthropicRequest(String systemPrompt, JsonArray messages, JsonArray tools) {
        JsonObject request = new JsonObject();
        if (isVertexAi()) {
            // Vertex AI: model is in the URL, version goes in body
            request.put("anthropic_version", VERTEX_ANTHROPIC_VERSION);
        } else {
            // Direct Anthropic API: model goes in body
            request.put("model", model);
        }
        request.put("max_tokens", maxTokens > 0 ? maxTokens : 4096);
        if (systemPrompt != null) {
            request.put("system", systemPrompt);
        }
        request.put("messages", messages);
        request.put("temperature", temperature);
        if (tools != null && !tools.isEmpty()) {
            request.put("tools", tools);
        }
        return request;
    }

    private String resolveAnthropicUrl() {
        if (isVertexAi()) {
            return String.format(
                    "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/anthropic/models/%s:rawPredict",
                    vertexRegion, vertexProjectId, vertexRegion, model);
        }
        String base = url != null ? url : DEFAULT_ANTHROPIC_URL;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/v1/messages";
    }

    private Map<String, String> buildAnthropicHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (isVertexAi()) {
            String token = getGcloudAccessToken();
            if (token != null) {
                headers.put("Authorization", "Bearer " + token);
            }
        } else {
            String key = resolveApiKey();
            if (key != null) {
                headers.put("x-api-key", key);
            }
            headers.put("anthropic-version", ANTHROPIC_VERSION);
        }
        return headers;
    }

    private boolean isVertexAi() {
        return vertexRegion != null && vertexProjectId != null;
    }

    private String getGcloudAccessToken() {
        try {
            ProcessBuilder pb = new ProcessBuilder("gcloud", "auth", "print-access-token");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String token = reader.readLine();
                int exit = p.waitFor();
                if (exit == 0 && token != null && !token.isBlank()) {
                    return token.strip();
                }
            }
        } catch (Exception e) {
            // gcloud not available
        }
        return null;
    }

    // ---- Response parsing ----

    private ChatResponse parseOpenAiChatResponse(JsonObject response) {
        if (response == null) {
            return new ChatResponse(null, List.of(), "error");
        }
        JsonArray choices = (JsonArray) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return new ChatResponse(null, List.of(), "error");
        }
        JsonObject firstChoice = (JsonObject) choices.get(0);
        String finishReason = firstChoice.getString("finish_reason");
        JsonObject message = (JsonObject) firstChoice.get("message");
        if (message == null) {
            return new ChatResponse(null, List.of(), finishReason);
        }

        String content = message.getString("content");
        JsonArray rawToolCalls = (JsonArray) message.get("tool_calls");
        List<ToolCall> toolCalls = new ArrayList<>();
        if (rawToolCalls != null) {
            for (Object obj : rawToolCalls) {
                JsonObject tc = (JsonObject) obj;
                JsonObject function = (JsonObject) tc.get("function");
                String id = tc.getString("id");
                String name = function.getString("name");
                JsonObject args;
                try {
                    args = (JsonObject) Jsoner.deserialize(function.getString("arguments"));
                } catch (Exception e) {
                    args = new JsonObject();
                }
                toolCalls.add(new ToolCall(id, name, args));
            }
        }
        return new ChatResponse(content, toolCalls, finishReason);
    }

    private ChatResponse parseAnthropicChatResponse(JsonObject response) {
        if (response == null) {
            return new ChatResponse(null, List.of(), "error");
        }
        String stopReason = response.getString("stop_reason");
        JsonArray contentBlocks = (JsonArray) response.get("content");
        if (contentBlocks == null) {
            return new ChatResponse(null, List.of(), stopReason);
        }

        StringBuilder text = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();
        for (Object obj : contentBlocks) {
            JsonObject block = (JsonObject) obj;
            String type = block.getString("type");
            if ("text".equals(type)) {
                text.append(block.getString("text"));
            } else if ("tool_use".equals(type)) {
                String id = block.getString("id");
                String name = block.getString("name");
                JsonObject input = (JsonObject) block.get("input");
                toolCalls.add(new ToolCall(id, name, input != null ? input : new JsonObject()));
            }
        }
        String textContent = text.length() > 0 ? text.toString() : null;
        return new ChatResponse(textContent, toolCalls, stopReason);
    }

    private String extractOpenAiContent(JsonObject response) {
        if (response == null) {
            return null;
        }
        JsonArray choices = (JsonArray) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        JsonObject firstChoice = (JsonObject) choices.get(0);
        JsonObject message = (JsonObject) firstChoice.get("message");
        return message != null ? message.getString("content") : null;
    }

    private String extractAnthropicTextContent(JsonObject response) {
        if (response == null) {
            return null;
        }
        JsonArray contentBlocks = (JsonArray) response.get("content");
        if (contentBlocks == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object obj : contentBlocks) {
            JsonObject block = (JsonObject) obj;
            if ("text".equals(block.getString("type"))) {
                sb.append(block.getString("text"));
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // ---- HTTP transport ----

    private JsonObject sendRequest(String requestUrl, JsonObject body, String authKey) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJson()));

            if (authKey != null && !authKey.isBlank()) {
                builder.header("Authorization", "Bearer " + authKey);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return (JsonObject) Jsoner.deserialize(response.body());
            }

            handleErrorStatus(response.statusCode(), response.body());
            return null;
        } catch (HttpTimeoutException e) {
            printer.println("Request timed out after " + timeout + " seconds.");
            return null;
        } catch (Exception e) {
            printer.println("Error calling LLM: " + e.getMessage());
            return null;
        }
    }

    private JsonObject sendRequestWithHeaders(String requestUrl, JsonObject body, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(timeout))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJson()));

            for (Map.Entry<String, String> h : headers.entrySet()) {
                builder.header(h.getKey(), h.getValue());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return (JsonObject) Jsoner.deserialize(response.body());
            }

            handleErrorStatus(response.statusCode(), response.body());
            return null;
        } catch (HttpTimeoutException e) {
            printer.println("Request timed out after " + timeout + " seconds.");
            return null;
        } catch (Exception e) {
            printer.println("Error calling LLM: " + e.getMessage());
            return null;
        }
    }

    String sendStreamingRequest(String requestUrl, JsonObject body, String authKey, String textField) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJson()));

            if (authKey != null && !authKey.isBlank()) {
                builder.header("Authorization", "Bearer " + authKey);
            }

            HttpResponse<Stream<String>> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                handleErrorStatus(response.statusCode(), "Streaming request failed");
                return null;
            }

            StringBuilder fullResponse = new StringBuilder();
            response.body().forEach(line -> {
                if (line.isBlank()) {
                    return;
                }
                try {
                    JsonObject chunk = (JsonObject) Jsoner.deserialize(line);
                    String text = chunk.getString(textField);
                    if (text != null) {
                        printer.print(text);
                        fullResponse.append(text);
                    }
                } catch (Exception e) {
                    // skip malformed chunks
                }
            });
            printer.println();
            return fullResponse.toString();
        } catch (HttpTimeoutException e) {
            printer.println("\nRequest timed out after " + timeout + " seconds.");
            return null;
        } catch (Exception e) {
            printer.println("\nError during streaming: " + e.getMessage());
            return null;
        }
    }

    private String sendAnthropicStreamingRequest(String requestUrl, JsonObject body, Map<String, String> headers) {
        body.put("stream", true);
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(timeout))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJson()));

            for (Map.Entry<String, String> h : headers.entrySet()) {
                builder.header(h.getKey(), h.getValue());
            }

            HttpResponse<Stream<String>> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                handleErrorStatus(response.statusCode(), "Streaming request failed");
                return null;
            }

            StringBuilder fullResponse = new StringBuilder();
            response.body().forEach(line -> {
                if (line.isBlank() || !line.startsWith("data: ")) {
                    return;
                }
                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    return;
                }
                try {
                    JsonObject event = (JsonObject) Jsoner.deserialize(data);
                    String type = event.getString("type");
                    if ("content_block_delta".equals(type)) {
                        JsonObject delta = (JsonObject) event.get("delta");
                        if (delta != null && "text_delta".equals(delta.getString("type"))) {
                            String text = delta.getString("text");
                            if (text != null) {
                                printer.print(text);
                                fullResponse.append(text);
                            }
                        }
                    }
                } catch (Exception e) {
                    // skip malformed events
                }
            });
            printer.println();
            return fullResponse.toString();
        } catch (HttpTimeoutException e) {
            printer.println("\nRequest timed out after " + timeout + " seconds.");
            return null;
        } catch (Exception e) {
            printer.println("\nError during streaming: " + e.getMessage());
            return null;
        }
    }

    // ---- Endpoint detection helpers ----

    private boolean tryExplicitUrl() {
        if (url == null || url.isBlank()) {
            return false;
        }
        return isEndpointReachable(url);
    }

    private boolean tryAnthropicApiKey() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            apiType = ApiType.anthropic;
            apiKey = key;
            url = DEFAULT_ANTHROPIC_URL;
            if (model == null || "llama3.2".equals(model)) {
                model = DEFAULT_ANTHROPIC_MODEL;
            }
            return true;
        }
        return false;
    }

    private boolean tryVertexAi() {
        String region = System.getenv("CLOUD_ML_REGION");
        String project = System.getenv("ANTHROPIC_VERTEX_PROJECT_ID");
        if (region != null && !region.isBlank() && project != null && !project.isBlank()) {
            apiType = ApiType.anthropic;
            vertexRegion = region;
            vertexProjectId = project;
            if (model == null || "llama3.2".equals(model)) {
                model = DEFAULT_ANTHROPIC_MODEL;
            }
            return true;
        }
        return false;
    }

    private boolean tryAnthropicOrVertex() {
        if (url != null && !url.isBlank()) {
            return isEndpointReachable(url);
        }
        return tryAnthropicApiKey() || tryVertexAi();
    }

    private boolean tryOpenAi() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("LLM_API_KEY");
        }
        if (key != null && !key.isBlank()) {
            apiType = ApiType.openai;
            apiKey = key;
            if (url == null || url.isBlank()) {
                url = "https://api.openai.com";
            }
            return true;
        }
        return false;
    }

    private boolean tryInfraOllama() {
        try {
            Map<Long, Path> pids = findOllamaPids();
            for (Path pidFile : pids.values()) {
                String baseUrl = readBaseUrlFromPidFile(pidFile);
                if (baseUrl != null && isEndpointReachable(baseUrl)) {
                    apiType = ApiType.ollama;
                    url = baseUrl;
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private boolean tryDefaultOllama() {
        if (isEndpointReachable(DEFAULT_OLLAMA_URL)) {
            apiType = ApiType.ollama;
            url = DEFAULT_OLLAMA_URL;
            return true;
        }
        return false;
    }

    boolean isEndpointReachable(String endpoint) {
        return tryHealthCheck(endpoint + "/api/tags")
                || tryHealthCheck(endpoint + "/v1/models")
                || tryHealthCheck(endpoint);
    }

    private boolean tryHealthCheck(String healthUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(HEALTH_CHECK_TIMEOUT_SECONDS))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<Long, Path> findOllamaPids() throws Exception {
        Map<Long, Path> pids = new HashMap<>();
        Path camelDir = CommandLineHelper.getCamelDir();
        if (!Files.exists(camelDir)) {
            return pids;
        }
        try (Stream<Path> fileStream = Files.list(camelDir)) {
            fileStream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("infra-ollama-") && name.endsWith(".json");
                    })
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        String pidStr = name.substring(name.lastIndexOf("-") + 1, name.lastIndexOf('.'));
                        try {
                            pids.put(Long.valueOf(pidStr), p);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    });
        }
        return pids;
    }

    private String readBaseUrlFromPidFile(Path pidFile) throws Exception {
        String json = Files.readString(pidFile);
        JsonObject jo = (JsonObject) Jsoner.deserialize(json);
        return jo.getString("baseUrl");
    }

    // ---- URL helpers ----

    String normalizeOpenAiUrl(String endpoint) {
        String u = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        if (!u.endsWith("/v1/chat/completions")) {
            u = u.endsWith("/v1") ? u : u + "/v1";
            u = u + "/chat/completions";
        }
        return u;
    }

    // ---- Error handling ----

    private void handleErrorStatus(int statusCode, String body) {
        printer.println("LLM returned status: " + statusCode);
        switch (statusCode) {
            case 401 -> printer.println("Authentication failed. Check your API key.");
            case 429 -> printer.println("Rate limit exceeded.");
            default -> {
            }
        }
        if (body != null && !body.isBlank()) {
            printer.println(body);
        }
    }

    // ---- OpenAI message helpers ----

    static JsonObject createOpenAiMessage(String role, String content) {
        JsonObject msg = new JsonObject();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }
}
