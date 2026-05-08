package com.aiminion.aiservice.common.ai.generator;

import com.aiminion.aiservice.common.ai.context.UserGeminiApiKeyContext;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GeminiImageGenerator {
    private static final String FALLBACK_IMAGE_MODEL = "gemini-3-pro-image-preview";
    private static final AtomicInteger MODEL_ROTATION_INDEX = new AtomicInteger(0);
    private static final Pattern API_BASE_PATTERN =
            Pattern.compile("^(https?://[^/]+)/(v1(?:beta)?)/models/?$");
    private static final List<String> API_VERSIONS = List.of("v1beta", "v1");
    private static final List<String> ROTATING_IMAGE_MODELS = List.of(
            "gemini-3.1-flash-image-preview",
            "gemini-3-pro-image-preview"
    );
    private static final List<String> KNOWN_IMAGE_MODEL_CANDIDATES = List.of(
            "gemini-3.1-flash-image-preview",
            "gemini-3-pro-image-preview",
            "gemini-2.0-flash-preview-image-generation",
            "gemini-2.5-flash-image-preview",
            "gemini-2.5-flash-image-preview-04-17",
            "imagen-3.0-generate-002"
    );


    private final RestTemplate restTemplate;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.api-url}")
    private String apiUrl;

    @Value("${gemini.image-model}")
    private String imageModel;

    public GeminiImageGenerator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GeneratedImage generateToonImage(
            String prompt,
            String size,
            String toonStyle,
            String shortText,
            String logoDataUri,
            String photoDataUri,
            String contentTypeHint,
            String toneHint
    ) {
        String creativeBrief = formatCreativeBrief(contentTypeHint, toneHint);
        String enhancedPrompt = """
                Create a high-quality image suitable for social sharing.
                %sFollow the Style hint for art direction (line work, color, lighting, level of realism vs illustration). Do not contradict it.
                Keep composition clear and readable at a glance unless the Style hint calls for a different treatment.
                Do not add any watermark.
                Do not render any readable text inside the artwork.
                No speech bubbles, no signs, no labels, no letters, and no numbers.
                Do not put any writing on clothes, books, boards, walls, or objects.
                If a book appears, keep the cover plain with simple shapes only (no script).
                The only allowed text should come from externally overlaid caption/logo if applied later.
                If speech/dialogue is unavoidable for scene realism, use correct Burmese Unicode only.
                Aspect hint: %s.
                Style hint: %s.
                Scene request: %s
                """.formatted(creativeBrief, size, normalizeStyle(toonStyle), prompt);

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", enhancedPrompt));

        Map<String, Object> logoInline = tryBuildInlineData(logoDataUri);
        if (logoInline != null) {
            parts.add(Map.of("text", """
                    Use the provided reference image as the logo.
                    Preserve logo exactly as uploaded: do not redraw, restyle, recolor, blur, crop, or change text/shapes.
                    Only placement and uniform scaling are allowed.
                    Preferred placement: top-right.
                    """));
            parts.add(Map.of("inlineData", logoInline));
        }
        Map<String, Object> photoInline = tryBuildInlineData(photoDataUri);
        if (photoInline != null) {
            parts.add(Map.of("text", "Use the provided reference photo as a character/object in the scene."));
            parts.add(Map.of("inlineData", photoInline));
        }

        List<String> candidates = buildModelCandidates();
        List<ModelCapability> capabilities = discoverModelCapabilities();
        List<String> errors = new ArrayList<>();
        log.info("[GeminiImageGenerator] Model candidates (in order): {}", candidates);

        for (String candidate : candidates) {
            try {
                log.info("[GeminiImageGenerator] Trying model={}", candidate);
                return executeWithModel(candidate, enhancedPrompt, parts, capabilities);
            } catch (HttpClientErrorException ex) {
                String snippet = ex.getResponseBodyAsString();
                String err = candidate + " -> HTTP " + ex.getStatusCode().value()
                        + " " + shortMsg(snippet);
                errors.add(err);
                log.warn("[GeminiImageGenerator] Image model attempt failed: {}", err);
            } catch (Exception ex) {
                String err = candidate + " -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
                errors.add(err);
                log.warn("[GeminiImageGenerator] Image model attempt failed: {}", err);
            }
        }

        throw new RuntimeException("Gemini image generation failed for all model candidates: " + String.join(" | ", errors));
    }

    private GeneratedImage executeWithModel(
            String model,
            String prompt,
            List<Map<String, Object>> parts,
            List<ModelCapability> capabilities
    ) {
        List<EndpointAttempt> attempts = buildEndpointAttempts(model, prompt, parts, capabilities);
        List<String> errors = new ArrayList<>();

        for (EndpointAttempt attempt : attempts) {
            try {
                log.info("[GeminiImageGenerator] Attempt model={} method={} version={}",
                        model, attempt.method, attempt.version);
                return executeAttempt(attempt);
            } catch (HttpClientErrorException ex) {
                String err = attempt.method + "@" + attempt.version + " -> HTTP "
                        + ex.getStatusCode().value() + " " + shortMsg(ex.getResponseBodyAsString());
                errors.add(err);
            } catch (Exception ex) {
                String err = attempt.method + "@" + attempt.version + " -> "
                        + ex.getClass().getSimpleName() + ": " + ex.getMessage();
                errors.add(err);
            }
        }
        throw new RuntimeException(model + " exhausted endpoint attempts: " + String.join(" | ", errors));
    }

    private GeneratedImage executeAttempt(EndpointAttempt attempt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<?> response = restTemplate.postForEntity(
                attempt.url, new HttpEntity<>(attempt.body, headers), Object.class
        );
        if (!(response.getBody() instanceof Map<?, ?> mapBody)) {
            throw new RuntimeException("Unexpected Gemini image response payload");
        }
        GeneratedImage generated = "generateImages".equals(attempt.method)
                ? extractImageFromGenerateImages(mapBody)
                : extractImageFromGenerateContent(mapBody);
        log.info("[GeminiImageGenerator] Success model={} method={} version={} imageName={}",
                attempt.model, attempt.method, attempt.version, generated.imageName());
        return generated;
    }

    private List<String> buildModelCandidates() {
        Set<String> ordered = new LinkedHashSet<>();
        ordered.addAll(rotatingModelsForThisRequest());
        if (imageModel != null && !imageModel.isBlank()) {
            ordered.add(imageModel.trim());
        }
        ordered.add(FALLBACK_IMAGE_MODEL);
        ordered.addAll(KNOWN_IMAGE_MODEL_CANDIDATES);
        ordered.addAll(discoverImageModels());
        return new ArrayList<>(ordered);
    }

    private List<String> rotatingModelsForThisRequest() {
        if (ROTATING_IMAGE_MODELS.isEmpty()) {
            return List.of();
        }
        int start = Math.floorMod(
                MODEL_ROTATION_INDEX.getAndIncrement(),
                ROTATING_IMAGE_MODELS.size()
        );
        List<String> rotated = new ArrayList<>(ROTATING_IMAGE_MODELS);
        Collections.rotate(rotated, -start);
        return rotated;
    }

    private List<String> discoverImageModels() {
        List<ModelCapability> capabilities = discoverModelCapabilities();
        Set<String> discovered = new LinkedHashSet<>();
        for (ModelCapability capability : capabilities) {
            String lower = capability.modelId().toLowerCase();
            if (lower.contains("image") || lower.contains("imagen")) {
                discovered.add(capability.modelId());
            }
        }
        return new ArrayList<>(discovered);
    }

    private List<ModelCapability> discoverModelCapabilities() {
        try {
            Map<String, Set<String>> methodsByVersionAndModel = new HashMap<>();
            for (String version : API_VERSIONS) {
                String modelsUrl = buildModelsUrl(version);
                ResponseEntity<?> response = restTemplate.getForEntity(modelsUrl, Object.class);
                if (!(response.getBody() instanceof Map<?, ?> mapBody)) {
                    continue;
                }
                Object modelsObj = mapBody.get("models");
                if (!(modelsObj instanceof List<?> rawModels)) {
                    continue;
                }
                for (Object raw : rawModels) {
                    if (!(raw instanceof Map<?, ?> modelInfo)) {
                        continue;
                    }
                    String name = modelInfo.get("name") instanceof String s ? s : null;
                    if (name == null || !name.startsWith("models/")) {
                        continue;
                    }
                    String modelId = name.substring("models/".length());

                    Set<String> supportedMethods = new HashSet<>();
                    Object methodsObj = modelInfo.get("supportedGenerationMethods");
                    if (methodsObj instanceof List<?> methods) {
                        for (Object method : methods) {
                            String m = String.valueOf(method);
                            if ("generateContent".equals(m) || "generateImages".equals(m)) {
                                supportedMethods.add(m);
                            }
                        }
                    }
                    if (supportedMethods.isEmpty()) {
                        continue;
                    }

                    methodsByVersionAndModel.put(version + "||" + modelId, supportedMethods);
                }
            }
            List<ModelCapability> capabilities = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : methodsByVersionAndModel.entrySet()) {
                String[] key = entry.getKey().split("\\|\\|", 2);
                capabilities.add(new ModelCapability(key[0], key[1], entry.getValue()));
            }
            return capabilities;
        } catch (Exception ex) {
            log.debug("[GeminiImageGenerator] ListModels discovery skipped: {}", ex.getMessage());
            return List.of();
        }
    }

    private String shortMsg(String message) {
        if (message == null || message.isBlank()) {
            return "(no body)";
        }
        String oneLine = message.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() <= 200 ? oneLine : oneLine.substring(0, 200) + "...";
    }

    /** Default comic / western graphic-novel toon preset (keep aligned with frontend {@code DEFAULT_TOON_STYLE}). */
    private static final String DEFAULT_TOON_STYLE_FALLBACK =
            "Western graphic novel toon: heavy bold outlines, aggressive shading with optional cross-hatching, high-energy composition; "
                    + "strong black linework defining forms, dramatic speed-line or energetic background, bold saturated colors flatter than photo realism, "
                    + "intense expressive faces.";

    /**
     * Optional copy-format and tone from content v2 — steers scene composition and atmosphere without adding readable text in-frame.
     */
    private static String formatCreativeBrief(String contentType, String tone) {
        String ct = contentType == null ? "" : contentType.trim();
        String tn = tone == null ? "" : tone.trim();
        if (ct.isEmpty() && tn.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder("Creative direction: ");
        if (!ct.isEmpty()) {
            b.append("Content format role is ")
                    .append(ct)
                    .append(" (interpret as a single clear social-ready visual; do not paint caption text into the image). ");
        }
        if (!tn.isEmpty()) {
            b.append("Mood and energy should feel ").append(tn).append(". ");
        }
        b.append("\n");
        return b.toString();
    }

    private String normalizeStyle(String toonStyle) {
        if (toonStyle == null || toonStyle.isBlank()) {
            return DEFAULT_TOON_STYLE_FALLBACK;
        }
        return toonStyle.trim();
    }

    private Map<String, Object> tryBuildInlineData(String dataUri) {
        if (dataUri == null || dataUri.isBlank() || !dataUri.startsWith("data:image")) {
            return null;
        }
        int comma = dataUri.indexOf(',');
        if (comma < 0) {
            return null;
        }
        String metadata = dataUri.substring(0, comma);
        String payload = dataUri.substring(comma + 1).replaceAll("\\s+", "");
        int semi = metadata.indexOf(';');
        String mime = semi > "data:".length()
                ? metadata.substring("data:".length(), semi)
                : "image/png";
        try {
            Base64.getMimeDecoder().decode(payload);
            return Map.of("mimeType", mime, "data", payload);
        } catch (IllegalArgumentException ex) {
            try {
                Base64.getUrlDecoder().decode(payload);
                String normalized = payload.replace('-', '+').replace('_', '/');
                return Map.of("mimeType", mime, "data", normalized);
            } catch (IllegalArgumentException ignored) {
                log.warn("[GeminiImageGenerator] Skip invalid data URI reference image");
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private GeneratedImage extractImageFromGenerateContent(Map<?, ?> body) {
        try {
            List<Map<?, ?>> candidates = (List<Map<?, ?>>) body.get("candidates");
            for (Map<?, ?> candidate : candidates) {
                Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                List<Map<?, ?>> parts = (List<Map<?, ?>>) content.get("parts");
                for (Map<?, ?> part : parts) {
                    Object inline = part.get("inlineData");
                    if (inline instanceof Map<?, ?> inlineData) {
                        String b64 = (String) inlineData.get("data");
                        String mime = inlineData.get("mimeType") instanceof String m ? m : "image/png";
                        if (b64 != null && !b64.isBlank()) {
                            byte[] bytes = Base64.getDecoder().decode(b64);
                            return GeneratedImage.builder()
                                    .bytes(bytes)
                                    .mimeType(mime)
                                    .imageName("img_v2_" + Instant.now().getEpochSecond() + ".png")
                                    .build();
                        }
                    }
                }
            }
            log.error("[GeminiImageGenerator] Unexpected response: {}", body);
            throw new RuntimeException("Gemini did not return an image. Try another prompt.");
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[GeminiImageGenerator] Parse failed: {}", body, ex);
            throw new RuntimeException("Unexpected response format from Gemini image API.");
        }
    }

    private GeneratedImage extractImageFromGenerateImages(Map<?, ?> body) {
        try {
            Object generatedImagesObj = body.get("generatedImages");
            if (!(generatedImagesObj instanceof List<?> generatedImages)) {
                throw new RuntimeException("Gemini did not return generatedImages.");
            }
            for (Object item : generatedImages) {
                if (!(item instanceof Map<?, ?> imageItem)) {
                    continue;
                }
                Object imageObj = imageItem.get("image");
                if (!(imageObj instanceof Map<?, ?> image)) {
                    continue;
                }
                String b64 = image.get("imageBytes") instanceof String s ? s : null;
                String mime = image.get("mimeType") instanceof String m ? m : "image/png";
                if (b64 != null && !b64.isBlank()) {
                    byte[] bytes = Base64.getDecoder().decode(b64);
                    return GeneratedImage.builder()
                            .bytes(bytes)
                            .mimeType(mime)
                            .imageName("img_v2_" + Instant.now().getEpochSecond() + ".png")
                            .build();
                }
            }
            throw new RuntimeException("Gemini generateImages returned no imageBytes.");
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[GeminiImageGenerator] generateImages parse failed: {}", body, ex);
            throw new RuntimeException("Unexpected response format from Gemini generateImages API.");
        }
    }

    private List<EndpointAttempt> buildEndpointAttempts(
            String model,
            String prompt,
            List<Map<String, Object>> parts,
            List<ModelCapability> capabilities
    ) {
        List<EndpointAttempt> attempts = new ArrayList<>();
        Map<String, Set<String>> byVersion = new HashMap<>();
        for (ModelCapability capability : capabilities) {
            if (model.equals(capability.modelId())) {
                byVersion.put(capability.version(), capability.supportedMethods());
            }
        }

        for (String version : API_VERSIONS) {
            Set<String> methods = byVersion.get(version);
            if (methods == null || methods.isEmpty()) {
                // Unknown capabilities: prefer safer attempt only.
                if ("v1beta".equals(version)) {
                    attempts.add(new EndpointAttempt(
                            model,
                            version,
                            "generateContent",
                            buildModelMethodUrl(version, model, "generateContent"),
                            buildGenerateContentBody(version, parts)
                    ));
                }
                continue;
            }
            if (methods.contains("generateContent")) {
                attempts.add(new EndpointAttempt(
                        model,
                        version,
                        "generateContent",
                        buildModelMethodUrl(version, model, "generateContent"),
                        buildGenerateContentBody(version, parts)
                ));
            }
            if (methods.contains("generateImages")) {
                attempts.add(new EndpointAttempt(
                        model,
                        version,
                        "generateImages",
                        buildModelMethodUrl(version, model, "generateImages"),
                        Map.of("prompt", Map.of("text", prompt))
                ));
            }
        }
        return attempts;
    }

    private Map<String, Object> buildGenerateContentBody(String version, List<Map<String, Object>> parts) {
        if ("v1beta".equals(version)) {
            return Map.of(
                    "contents", List.of(Map.of("parts", parts)),
                    "generationConfig", Map.of(
                            "temperature", 0.4,
                            "responseModalities", List.of("TEXT", "IMAGE")
                    )
            );
        }
        // v1 rejects responseModalities for many models/accounts.
        return Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of(
                        "temperature", 0.4
                )
        );
    }

    private String buildModelsUrl(String version) {
        String host = deriveApiHost();
        return UriComponentsBuilder
                .fromUriString(host + "/" + version + "/models")
                .queryParam("key", resolveApiKey())
                .build(true)
                .toUriString();
    }

    private String buildModelMethodUrl(String version, String model, String method) {
        String host = deriveApiHost();
        return UriComponentsBuilder
                .fromUriString(host + "/" + version + "/models/" + model + ":" + method)
                .queryParam("key", resolveApiKey())
                .build(true)
                .toUriString();
    }

    private String deriveApiHost() {
        String normalized = apiUrl == null ? "" : apiUrl.trim().replaceAll("/+$", "");
        Matcher matcher = API_BASE_PATTERN.matcher(normalized);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        if (normalized.contains("/v1beta/models")) {
            return normalized.substring(0, normalized.indexOf("/v1beta/models"));
        }
        if (normalized.contains("/v1/models")) {
            return normalized.substring(0, normalized.indexOf("/v1/models"));
        }
        throw new IllegalStateException("Invalid gemini.api-url. Expected .../v1beta/models/ or .../v1/models/");
    }

    private String resolveApiKey() {
        String fromUser = UserGeminiApiKeyContext.get();
        if (fromUser != null && !fromUser.isBlank()) {
            return fromUser.trim();
        }
        return apiKey;
    }

    private record EndpointAttempt(
            String model,
            String version,
            String method,
            String url,
            Map<String, Object> body
    ) {
    }

    private record ModelCapability(
            String version,
            String modelId,
            Set<String> supportedMethods
    ) {
    }

    @Builder
    public record GeneratedImage(
            byte[] bytes,
            String mimeType,
            String imageName
    ) {}
}
