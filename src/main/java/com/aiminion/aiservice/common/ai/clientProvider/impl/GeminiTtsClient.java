package com.aiminion.aiservice.common.ai.clientProvider.impl;

import com.aiminion.aiservice.common.ai.clientProvider.TtsClient;
import com.aiminion.aiservice.common.enums.AiProvider;
import com.aiminion.aiservice.feature.response.VoiceOverResponse;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Slf4j
@Component
public class GeminiTtsClient implements TtsClient {

    private final RestTemplate restTemplate;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.tts-model:gemini-2.5-flash-preview-tts}")
    private String ttsModel;

    /**
     * Used only when Gemini returns raw PCM (no container header).
     * Gemini TTS commonly emits 24kHz mono PCM16LE in inlineData.
     */
    @Value("${gemini.tts-sample-rate-hz:24000}")
    private int pcmSampleRateHz;

    private static final String GEMINI_TTS_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final Set<String> SUPPORTED_GEMINI_VOICES = Set.of(
            "achernar", "achird", "algenib", "algieba", "alnilam", "aoede", "autonoe", "callirrhoe",
            "charon", "despina", "enceladus", "erinome", "fenrir", "gacrux", "iapetus", "kore",
            "laomedeia", "leda", "orus", "puck", "pulcherrima", "rasalgethi", "sadachbia",
            "sadaltager", "schedar", "sulafat", "umbriel", "vindemiatrix", "zephyr", "zubenelgenubi"
    );

    private static final Map<String, String> VOICE_ALIASES = buildVoiceAliases();
    private static final int MAX_TTS_JSON_STRING_LENGTH = 50_000_000;
    private static final ObjectMapper LARGE_JSON_MAPPER = new ObjectMapper(
            JsonFactory.builder()
                    .streamReadConstraints(
                            StreamReadConstraints.builder()
                                    .maxStringLength(MAX_TTS_JSON_STRING_LENGTH)
                                    .build())
                    .build());

    public GeminiTtsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

//    public GeminiTtsClient(@Qualifier("proxyRestTemplate") RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.GEMINI;
    }

    /**
     * @param voice Gemini prebuilt voice: Aoede | Charon | Fenrir | Kore | Puck | Zephyr …
     * @param speed reserved for future use (Gemini TTS doesn't expose speed yet)
     */
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

            log.info("[GeminiTtsClient] TTS success, requestedVoice={} resolvedVoice={} model={} mimeType={} bytes={} header={}",
                    voice, resolvedVoice, ttsModel, inlineAudio.mimeType(), bytes.length, headerPreview(bytes));

            byte[] finalAudio;

            if (isPcmAudio(inlineAudio.mimeType())) {
                finalAudio = pcm16leToWav(bytes, pcmSampleRateHz, 1);
            } else {
                finalAudio = bytes;
            }

            // token estimation
            int tokenIn = estimateTokens(text);
            int tokenOut = 0;

            return VoiceOverResponse.builder()
                    .audioByte(finalAudio)
                    .audioBase64(Base64.getEncoder().encodeToString(finalAudio))
                    .usedProvider(AiProvider.GEMINI)
                    .tokenIn(tokenIn)
                    .tokenOut(tokenOut)
                    .build();

        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.error("[GeminiTtsClient] TTS call failed: status={} body={}", ex.getStatusCode(), responseBody, ex);
            throw new RuntimeException("Gemini TTS request failed: " + summarizeRemoteError(responseBody));
        } catch (Exception ex) {
            log.error("[GeminiTtsClient] TTS call failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Gemini voice generation unavailable. Please try again later.");
        }
    }

    /**
     * Response path:
     * candidates[0] → content → parts[0] → inlineData → data (base64 audio)
     */
    private InlineAudio extractInlineAudio(String rawBody) {
        try {
            if (rawBody == null || rawBody.isBlank()) {
                throw new RuntimeException("Empty Gemini TTS response.");
            }
            JsonNode root = LARGE_JSON_MAPPER.readTree(rawBody);
            JsonNode inlineData = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("inlineData");
            String data = inlineData.path("data").asText(null);
            String mimeType = inlineData.path("mimeType").isMissingNode()
                    ? null
                    : inlineData.path("mimeType").asText(null);
            if (data == null || data.isBlank()) {
                throw new RuntimeException("Gemini TTS response missing inline audio data.");
            }
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
        // Gemini commonly returns: "audio/L16;codec=pcm;rate=24000"
        return m.contains("codec=pcm")
                || m.contains("audio/pcm")
                || m.contains("audio/l16")
                || m.contains("audio/lpcm");
    }

    private static String headerPreview(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "<empty>";
        int n = Math.min(bytes.length, 24);
        StringBuilder hex = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) {
            hex.append(String.format("%02x", bytes[i]));
        }
        String ascii = new String(bytes, 0, Math.min(bytes.length, 12), StandardCharsets.US_ASCII)
                .replaceAll("[^\\x20-\\x7E]", ".");
        return "ascii=" + ascii + " hex=" + hex;
    }

    /**
     * Minimal WAV (RIFF) wrapper for PCM16LE.
     */
    private static byte[] pcm16leToWav(byte[] pcm, int sampleRateHz, int channels) {
        int bitsPerSample = 16;
        int byteRate = sampleRateHz * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int subchunk2Size = pcm.length;
        int chunkSize = 36 + subchunk2Size;

        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[] { 'R', 'I', 'F', 'F' });
        header.putInt(chunkSize);
        header.put(new byte[] { 'W', 'A', 'V', 'E' });
        header.put(new byte[] { 'f', 'm', 't', ' ' });
        header.putInt(16); // PCM header size
        header.putShort((short) 1); // audio format = PCM
        header.putShort((short) channels);
        header.putInt(sampleRateHz);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPerSample);
        header.put(new byte[] { 'd', 'a', 't', 'a' });
        header.putInt(subchunk2Size);

        byte[] wav = new byte[44 + pcm.length];
        System.arraycopy(header.array(), 0, wav, 0, 44);
        System.arraycopy(pcm, 0, wav, 44, pcm.length);
        return wav;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 3.5);
    }

    private static Map<String, String> buildVoiceAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("woman", "kore");
        aliases.put("female", "kore");
        aliases.put("girl", "aoede");
        aliases.put("man", "charon");
        aliases.put("male", "charon");
        aliases.put("boy", "fenrir");
        return aliases;
    }

    private String resolveVoiceName(String requestedVoice) {
        String fallback = "kore";
        if (requestedVoice == null || requestedVoice.isBlank()) {
            return fallback;
        }
        String normalized = requestedVoice.trim().toLowerCase();
        String fromAlias = VOICE_ALIASES.getOrDefault(normalized, normalized);
        if (SUPPORTED_GEMINI_VOICES.contains(fromAlias)) {
            return fromAlias;
        }
        return fallback;
    }

    private String summarizeRemoteError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "invalid request";
        }
        String singleLine = responseBody.replace('\n', ' ').replace('\r', ' ').trim();
        if (singleLine.length() > 220) {
            return singleLine.substring(0, 220) + "...";
        }
        return singleLine;
    }
}