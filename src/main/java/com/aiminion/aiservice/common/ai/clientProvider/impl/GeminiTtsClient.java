package com.aiminion.aiservice.common.ai.clientProvider.impl;

import com.aiminion.aiservice.common.ai.clientProvider.TtsClient;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.feature.response.VoiceModelDescriptor;
import com.aiminion.aiservice.feature.response.VoiceOverResponse;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.aiminion.aiservice.common.util.AIStyles.GEMINI_VOICE_CATALOG;

/**
 * NOT a Spring @Component — instantiated explicitly by {@link com.aiminion.aiservice.common.config.GeminiTtsConfig}
 * so that multiple instances (one per model) can coexist and be wrapped by
 * {@link com.aiminion.aiservice.common.ai.clientProvider.RoundRobinTtsClient}.
 */
@Slf4j
public class GeminiTtsClient implements TtsClient {

    private final RestTemplate restTemplate;
    private final String       apiKey;
    private final String       ttsModel;
    private final int          pcmSampleRateHz;

    private static final String GEMINI_TTS_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final Set<String> SUPPORTED_GEMINI_VOICES = GEMINI_VOICE_CATALOG.stream()
            .map(VoiceModelDescriptor::id)
            .collect(Collectors.toUnmodifiableSet());

    private static final Map<String, String> VOICE_ALIASES = buildVoiceAliases();

    private static final int          MAX_TTS_JSON_STRING_LENGTH = 50_000_000;
    private static final ObjectMapper LARGE_JSON_MAPPER = new ObjectMapper(
            JsonFactory.builder()
                    .streamReadConstraints(
                            StreamReadConstraints.builder()
                                    .maxStringLength(MAX_TTS_JSON_STRING_LENGTH)
                                    .build())
                    .build());

    public GeminiTtsClient(RestTemplate restTemplate,
                           String apiKey,
                           String ttsModel,
                           int pcmSampleRateHz) {
        this.restTemplate    = restTemplate;
        this.apiKey          = apiKey;
        this.ttsModel        = ttsModel;
        this.pcmSampleRateHz = pcmSampleRateHz;
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.GEMINI;
    }

    @Override
    public List<VoiceModelDescriptor> listVoiceModels() {
        return GEMINI_VOICE_CATALOG;
    }

    @Override
    public VoiceOverResponse synthesize(String text, String voice, double speed) {
        String resolvedVoice = resolveVoiceName(voice);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", text)))
                ),
                "generationConfig", Map.of(
                        "responseModalities", List.of("AUDIO"),
                        "speechConfig", Map.of(
                                "voiceConfig", Map.of(
                                        "prebuiltVoiceConfig", Map.of(
                                                "voiceName", resolvedVoice
                                        )
                                )
                        )
                )
        );

        String url = String.format(GEMINI_TTS_URL, ttsModel, apiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            InlineAudio inlineAudio = extractInlineAudio(response.getBody());
            byte[] bytes = Base64.getDecoder().decode(inlineAudio.base64Data());

            log.info("[GeminiTtsClient] TTS success model={} voice={} resolvedVoice={} mimeType={} bytes={} header={}",
                    ttsModel, voice, resolvedVoice, inlineAudio.mimeType(), bytes.length, headerPreview(bytes));

            byte[] finalAudio = isPcmAudio(inlineAudio.mimeType())
                    ? pcm16leToWav(bytes, pcmSampleRateHz, 1)
                    : bytes;

            return VoiceOverResponse.builder()
                    .audioByte(finalAudio)
                    .audioBase64(Base64.getEncoder().encodeToString(finalAudio))
                    .usedProvider(AiProvider.GEMINI)
                    .tokenIn(estimateTokens(text))
                    .tokenOut(0)
                    .build();

        } catch (HttpStatusCodeException ex) {
            log.error("[GeminiTtsClient] model={} status={} body={}",
                    ttsModel, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new RuntimeException("Gemini TTS request failed: " + summarizeRemoteError(ex.getResponseBodyAsString()));
        } catch (Exception ex) {
            log.error("[GeminiTtsClient] model={} error={}", ttsModel, ex.getMessage(), ex);
            throw new RuntimeException("Gemini voice generation unavailable. Please try again later.");
        }
    }

    // ── private helpers (unchanged from original) ─────────────────────────────

    private InlineAudio extractInlineAudio(String rawBody) {
        try {
            if (rawBody == null || rawBody.isBlank()) throw new RuntimeException("Empty Gemini TTS response.");
            JsonNode root = LARGE_JSON_MAPPER.readTree(rawBody);
            JsonNode inlineData = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("inlineData");
            String data     = inlineData.path("data").asText(null);
            String mimeType = inlineData.path("mimeType").isMissingNode()
                    ? null : inlineData.path("mimeType").asText(null);
            if (data == null || data.isBlank())
                throw new RuntimeException("Gemini TTS response missing inline audio data.");
            return new InlineAudio(data, mimeType);
        } catch (Exception ex) {
            log.error("[GeminiTtsClient] Failed to parse audio response: {}", summarizeRemoteError(rawBody));
            throw new RuntimeException("Unexpected response format from Gemini TTS.");
        }
    }

    private record InlineAudio(String base64Data, String mimeType) {}

    private static boolean isPcmAudio(String mimeType) {
        if (mimeType == null) return false;
        String m = mimeType.toLowerCase();
        return m.contains("codec=pcm") || m.contains("audio/pcm")
                || m.contains("audio/l16") || m.contains("audio/lpcm");
    }

    private static byte[] pcm16leToWav(byte[] pcm, int sampleRateHz, int channels) {
        int bitsPerSample = 16;
        int byteRate      = sampleRateHz * channels * bitsPerSample / 8;
        int blockAlign    = channels * bitsPerSample / 8;
        int subchunk2Size = pcm.length;
        int chunkSize     = 36 + subchunk2Size;

        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{'R','I','F','F'});   header.putInt(chunkSize);
        header.put(new byte[]{'W','A','V','E'});
        header.put(new byte[]{'f','m','t',' '});   header.putInt(16);
        header.putShort((short) 1);                header.putShort((short) channels);
        header.putInt(sampleRateHz);               header.putInt(byteRate);
        header.putShort((short) blockAlign);       header.putShort((short) bitsPerSample);
        header.put(new byte[]{'d','a','t','a'});   header.putInt(subchunk2Size);

        byte[] wav = new byte[44 + pcm.length];
        System.arraycopy(header.array(), 0, wav, 0, 44);
        System.arraycopy(pcm, 0, wav, 44, pcm.length);
        return wav;
    }

    private static String headerPreview(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "<empty>";
        int n = Math.min(bytes.length, 24);
        StringBuilder hex = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) hex.append(String.format("%02x", bytes[i]));
        String ascii = new String(bytes, 0, Math.min(bytes.length, 12), StandardCharsets.US_ASCII)
                .replaceAll("[^\\x20-\\x7E]", ".");
        return "ascii=" + ascii + " hex=" + hex;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 3.5);
    }

    private static Map<String, String> buildVoiceAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("woman", "kore");
        aliases.put("female", "kore");
        aliases.put("girl",  "aoede");
        aliases.put("man",    "charon");
        aliases.put("male",  "charon");
        aliases.put("boy",    "fenrir");
        return aliases;
    }

    private String resolveVoiceName(String requestedVoice) {
        String fallback = "kore";
        if (requestedVoice == null || requestedVoice.isBlank()) return fallback;
        String normalized = requestedVoice.trim().toLowerCase();
        String fromAlias  = VOICE_ALIASES.getOrDefault(normalized, normalized);
        return SUPPORTED_GEMINI_VOICES.contains(fromAlias) ? fromAlias : fallback;
    }

    private String summarizeRemoteError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "invalid request";
        String singleLine = responseBody.replace('\n', ' ').replace('\r', ' ').trim();
        return singleLine.length() > 220 ? singleLine.substring(0, 220) + "..." : singleLine;
    }
}