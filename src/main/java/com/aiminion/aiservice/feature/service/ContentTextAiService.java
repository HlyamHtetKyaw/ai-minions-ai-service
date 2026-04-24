package com.aiminion.aiservice.feature.service;

import com.aiminion.aiservice.common.ai.generator.AiContentTextGenerator;
import com.aiminion.aiservice.common.ai.handler.AiFeatureHandler;
import com.aiminion.aiservice.common.ai.prompt.impl.PromptBuilderImpl;
import com.aiminion.aiservice.common.ai.request.AiGenerateRequest;
import com.aiminion.aiservice.common.ai.request.AiRequest;
import com.aiminion.aiservice.common.ai.response.AiGenerateResponse;
import com.aiminion.aiservice.common.ai.response.AiResponse;
import com.aiminion.aiservice.common.enums.FeatureType;
import com.aiminion.aiservice.common.sanitizer.service.SanitizerService;
import com.aiminion.aiservice.feature.BaseAiServiceGenerator;
import com.aiminion.aiservice.feature.request.ContentTextRequest;
import com.aiminion.aiservice.feature.response.ContentTextResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentTextAiService
        implements BaseAiServiceGenerator<ContentTextRequest, ContentTextResponse>,
        AiFeatureHandler {

    private final AiContentTextGenerator aiContentTextGenerator;
    private final PromptBuilderImpl promptBuilderImpl;
    private final SanitizerService sanitizerService;

    private static final String DEFAULT_SOURCE = "English";
    private static final String DEFAULT_TARGET = "Myanmar";
    private static final String DEFAULT_STYLE  = "Formal";
    private static final String DEFAULT_CONTENT_TYPE = "Caption";

    @Override
    public FeatureType getFeatureType() {
        return FeatureType.GENERATE_CONTENT_TEXT;
    }

    @Override
    public AiGenerateResponse handle(AiGenerateRequest request, ObjectMapper objectMapper) {
        ContentTextRequest req = objectMapper.convertValue(request.payload(), ContentTextRequest.class);

        if (request.provider() != null) {
            req = ContentTextRequest.builder()
                    .topic(req.topic())
                    .contentType(req.contentType())
                    .sourceLanguage(req.sourceLanguage())
                    .targetLanguage(req.targetLanguage())
                    .style(req.style())
                    .textLength(req.textLength())
                    .provider(request.provider())
                    .build();
        }

        // Sanitizer for GENERATE_CONTENT_TEXT
        sanitizerService.sanitize(req.topic(), FeatureType.GENERATE_CONTENT_TEXT , request.provider());

        ContentTextResponse result = generate(req);

        return AiGenerateResponse.builder()
                .featureType(FeatureType.GENERATE_CONTENT_TEXT)
                .usedProvider(result.usedProvider())
                .result(result)
                .build();
    }

    @Override
    public ContentTextResponse generate(ContentTextRequest req) {
        String source = isBlank(req.sourceLanguage()) ? DEFAULT_SOURCE : req.sourceLanguage().trim();
        String target = isBlank(req.targetLanguage()) ? DEFAULT_TARGET : req.targetLanguage().trim();
        String contentType = isBlank(req.contentType()) ? DEFAULT_CONTENT_TYPE : req.contentType().trim();
        String style  = isBlank(req.style())          ? DEFAULT_STYLE  : req.style().trim();
        String textLength = isBlank(req.textLength()) ? "SHORT" : req.textLength().trim();

        log.info("[ContentText] {}→{} type={} length={} style={}", source, target, contentType, textLength, style);

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(promptBuilderImpl.buildContentTextPrompt(source, target, contentType, style, textLength))
                .userMessage(req.topic())
                .provider(req.provider())
                .build();

        AiResponse aiResponse = aiContentTextGenerator.generate(aiRequest);

        String rawContent = aiResponse.content();
        String title   = parseSection(rawContent, "TITLE");
        String content = parseSection(rawContent, "CONTENT");

        log.info("[ContentText] Parsed title='{}'", title);

        return ContentTextResponse.builder()
                .title(title)
                .contentType(contentType)
                .generatedFrom(source)
                .generatedTo(target)
                .style(style)
                .usedProvider(aiResponse.usedProvider())
                .generatedContent(content)
                .build();
    }

    /**
     * Only these headers delimit sections. Scripts/captions use many other bracketed lines
     * (e.g. {@code [Visual:]}, {@code [B-Roll:]}) — stopping at the next {@code [} would truncate
     * {@code [CONTENT]} to empty.
     */
    private static final String[] TOP_LEVEL_SECTION_MARKERS = {"[TITLE]:", "[CONTENT]:"};

    private static int findNextTopLevelSectionStart(String raw, int fromIndex) {
        int best = -1;
        for (String m : TOP_LEVEL_SECTION_MARKERS) {
            int idx = raw.indexOf(m, fromIndex);
            if (idx >= 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }

    /**
     * Extracts content between {@code [TAG]:} and the next top-level {@code [TITLE]:} / {@code [CONTENT]:}
     * (or end of string). e.g. "[TITLE]: My Title\n[CONTENT]: ..." → title "My Title".
     */
    private String parseSection(String raw, String tag) {
        String marker = "[" + tag + "]:";
        int start = raw.indexOf(marker);
        if (start == -1) {
            log.warn("[ContentText] Could not find tag [{}] in response", tag);
            return raw.trim();
        }
        start += marker.length();

        int end = findNextTopLevelSectionStart(raw, start);
        String section = end == -1 ? raw.substring(start) : raw.substring(start, end);
        return section.trim();
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}