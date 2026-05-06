package com.aiminion.aiservice.feature.imageOverlay.service;

import com.aiminion.aiservice.config.ImageOverlayConfig;
import com.aiminion.aiservice.feature.imageOverlay.request.OverlayRequest;
import com.aiminion.aiservice.feature.imageOverlay.response.OverlayResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Handles image composition — overlays logo and photo on a base image.
 * Positions/sizes are fully driven by ImageOverlayConfig.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageOverlayService {

    private final ImageOverlayConfig overlayConfig;
    private volatile List<Font> bundledFonts;
    private static final int REMOTE_IMAGE_MAX_ATTEMPTS = 4;

    public OverlayResult compose(OverlayRequest request) {
        try {
//            log.info("[ImageOverlay] Composing image from URL='{}'", request.baseImageUrl());

            // ── Load base image ───────────────────────────────────────────────
            BufferedImage base = loadImageFromUrl(request.baseImageUrl());
            Graphics2D    g2d  = base.createGraphics();

            // ── Enable antialiasing for smooth edges ──────────────────────────
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);

            // ── Overlay logo ──────────────────────────────────────────────────
            if (request.logoUrl() != null && !request.logoUrl().isBlank()) {
                ImageOverlayConfig.Logo cfg = overlayConfig.getLogo();   // ← Logo, not OverlayItem
                String position = resolve(request.logoPosition(), cfg.getPosition());
                log.info("[ImageOverlay] Overlaying logo at position={}", position);
                try {
                    overlayImage(g2d, base, request.logoUrl(),
                            resolve(request.logoWidth(),  cfg.getWidth()),
                            resolve(request.logoHeight(), cfg.getHeight()),
                            resolve(request.logoMargin(), cfg.getMargin()),
                            position);
                } catch (IOException ex) {
                    // Logo is optional. Keep generation resilient even when uploaded format is unsupported.
                    log.warn("[ImageOverlay] Skipping logo overlay: {}", ex.getMessage());
                }
            }

            // ── Overlay photo ─────────────────────────────────────────────────
            if (request.photoUrl() != null && !request.photoUrl().isBlank()) {
                ImageOverlayConfig.Photo cfg = overlayConfig.getPhoto();  // ← Photo, not OverlayItem
                String position = resolve(request.photoPosition(), cfg.getPosition());
                log.info("[ImageOverlay] Overlaying photo at position={}", position);
                try {
                    overlayImage(g2d, base, request.photoUrl(),
                            resolve(request.photoWidth(),  cfg.getWidth()),
                            resolve(request.photoHeight(), cfg.getHeight()),
                            resolve(request.photoMargin(), cfg.getMargin()),
                            position);
                } catch (IOException ex) {
                    // Photo is optional. Continue with base image and text when decoding fails.
                    log.warn("[ImageOverlay] Skipping photo overlay: {}", ex.getMessage());
                }
            }

            // ── Overlay caption text (AI and/or user) ─────────────────────────
            boolean hasAiText = request.aiShortText() != null && !request.aiShortText().isBlank();
            boolean hasUserText = request.userShortText() != null && !request.userShortText().isBlank();
            boolean hasLegacyText = request.shortText() != null && !request.shortText().isBlank();
            if (hasAiText || hasUserText || hasLegacyText) {
                overlayShortText(
                        g2d,
                        base,
                        hasAiText ? request.aiShortText() : (hasLegacyText ? request.shortText() : null),
                        request.userShortText(),
                        request.textPosition()
                );
            }

            g2d.dispose();

            // ── Convert to bytes ──────────────────────────────────────────────
            byte[] imageBytes = toBytes(base);
            String imageName  = "composed_" + Instant.now().getEpochSecond() + ".png";

//            log.info("[ImageOverlay] Composition complete — file={}", imageName);

            return OverlayResult.builder()
                    .imageBytes(imageBytes)
                    .imageName(imageName)
                    .build();

        } catch (Exception ex) {
            log.error("[ImageOverlay] Composition failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Image composition failed. Please try again.");
        }
    }

// ── Resolver helpers ──────────────────────────────────────────────────────────

    /** Uses user-supplied int if present, otherwise falls back to config default. */
    private int resolve(Integer userValue, int configDefault) {
        return userValue != null ? userValue : configDefault;
    }

    /** Uses user-supplied String if non-blank, otherwise falls back to config default. */
    private String resolve(String userValue, String configDefault) {
        return (userValue != null && !userValue.isBlank()) ? userValue : configDefault;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void overlayImage(Graphics2D g2d, BufferedImage base,
                              String imageUrl, int width, int height,
                              int margin, String position) throws IOException {

        BufferedImage overlay = loadImageFromUrl(imageUrl);
        BufferedImage scaled  = scaleImage(overlay, width, height);

        int[] coords = resolvePosition(position, base.getWidth(), base.getHeight(),
                width, height, margin);

        g2d.drawImage(scaled, coords[0], coords[1], null);
    }

    /**
     * Resolves (x, y) pixel coordinates based on named position.
     * To add a new position, just add a case here.
     */
    private int[] resolvePosition(String position,
                                  int baseWidth, int baseHeight,
                                  int overlayWidth, int overlayHeight,
                                  int margin) {
        return switch (position.toUpperCase()) {
            case "TOP_LEFT"     -> new int[]{ margin, margin };
            case "TOP_RIGHT"    -> new int[]{ baseWidth  - overlayWidth  - margin, margin };
            case "BOTTOM_LEFT"  -> new int[]{ margin, baseHeight - overlayHeight - margin };
            case "BOTTOM_RIGHT" -> new int[]{ baseWidth  - overlayWidth  - margin,
                    baseHeight - overlayHeight - margin };
            case "CENTER"       -> new int[]{ (baseWidth  - overlayWidth)  / 2,
                    (baseHeight - overlayHeight) / 2 };
            default -> {
                log.warn("[ImageOverlay] Unknown position '{}', defaulting to TOP_RIGHT", position);
                yield new int[]{ baseWidth - overlayWidth - margin, margin };
            }
        };
    }

    private BufferedImage scaleImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    private BufferedImage loadImageFromUrl(String url) throws IOException {
        if (url.startsWith("data:image")) {
            return loadImageFromDataUri(url);
        }
        IOException last = null;
        for (int attempt = 1; attempt <= REMOTE_IMAGE_MAX_ATTEMPTS; attempt++) {
            HttpURLConnection conn = null;
            try {
                URL imageUrl = URI.create(url).toURL();
                conn = (HttpURLConnection) imageUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);
                int status = conn.getResponseCode();
                if (status != 200) {
                    String message = "Failed to fetch image. HTTP Status: " + status;
                    if (attempt < REMOTE_IMAGE_MAX_ATTEMPTS && isRetryableStatus(status)) {
                        log.warn("[ImageOverlay] Remote image fetch retry {}/{} status={} url={}",
                                attempt, REMOTE_IMAGE_MAX_ATTEMPTS, status, url);
                        sleepBeforeRetry(attempt);
                        continue;
                    }
                    throw new IOException(message);
                }
                try (InputStream is = conn.getInputStream()) {
                    BufferedImage img = ImageIO.read(is);
                    if (img == null) {
                        throw new IOException("ImageIO.read returned null (invalid image format)");
                    }
                    return img;
                }
            } catch (IOException ex) {
                last = ex;
                if (attempt >= REMOTE_IMAGE_MAX_ATTEMPTS) {
                    break;
                }
                log.warn("[ImageOverlay] Remote image fetch retry {}/{} due to error: {}",
                        attempt, REMOTE_IMAGE_MAX_ATTEMPTS, ex.getMessage());
                sleepBeforeRetry(attempt);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        throw last == null ? new IOException("Failed to fetch remote image") : last;
    }

    private boolean isRetryableStatus(int status) {
        return status == 403 || status == 404 || status == 408 || status == 425 || status == 429 || status >= 500;
    }

    private void sleepBeforeRetry(int attempt) {
        long delayMs = switch (attempt) {
            case 1 -> 250L;
            case 2 -> 700L;
            default -> 1500L;
        };
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private byte[] toBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private BufferedImage loadImageFromDataUri(String dataUri) throws IOException {
        int comma = dataUri.indexOf(',');
        if (comma < 0) {
            throw new IOException("Invalid data URI image");
        }
        String metadata = dataUri.substring(0, comma);
        String payload = dataUri.substring(comma + 1);
        String mimeType = extractMimeType(metadata);
        boolean isBase64 = metadata.toLowerCase().contains(";base64");

        byte[] bytes = decodeDataUriPayload(payload, isBase64);
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                throw new IOException("Unsupported or invalid data URI image bytes (mime=%s)".formatted(mimeType));
            }
            return img;
        }
    }

    private byte[] decodeDataUriPayload(String payload, boolean isBase64) {
        if (!isBase64) {
            String decoded = URLDecoder.decode(payload, StandardCharsets.UTF_8);
            return decoded.getBytes(StandardCharsets.UTF_8);
        }

        String compact = payload.replaceAll("\\s+", "");
        try {
            return Base64.getMimeDecoder().decode(compact);
        } catch (IllegalArgumentException ex) {
            // Some clients may emit URL-safe Base64 in data URIs.
            return Base64.getUrlDecoder().decode(compact);
        }
    }

    private String extractMimeType(String metadata) {
        if (!metadata.startsWith("data:")) {
            return "unknown";
        }
        int semi = metadata.indexOf(';');
        if (semi < 0) {
            return metadata.substring("data:".length());
        }
        return metadata.substring("data:".length(), semi);
    }

    private void overlayShortText(Graphics2D g2d, BufferedImage base, String aiText, String userText, String textPosition) {
        String ai = aiText == null ? "" : aiText.trim();
        String user = userText == null ? "" : userText.trim();
        int width = base.getWidth();
        int height = base.getHeight();
        int fontSize = Math.max(30, width / 16);
        Font font = resolveFontForText(ai + user, fontSize);
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();
        int sidePadding = Math.max(20, width / 28);
        int maxTextWidth = width - (sidePadding * 2);
        List<StyledLine> styledLines = new ArrayList<>();
        if (!ai.isBlank()) {
            for (String line : wrapText(ai, fm, maxTextWidth, 2)) {
                styledLines.add(new StyledLine(line, Color.WHITE));
            }
        }
        if (!user.isBlank()) {
            for (String line : wrapText(user, fm, maxTextWidth, 2)) {
                styledLines.add(new StyledLine(line, new Color(255, 196, 246)));
            }
        }
        if (styledLines.isEmpty()) {
            return;
        }
        if (styledLines.size() > 4) {
            styledLines = new ArrayList<>(styledLines.subList(0, 4));
        }

        int lineHeight = fm.getHeight();
        int blockHeight = lineHeight * styledLines.size();
        int yBase = resolveTextY(textPosition, height, fm, blockHeight);

        int bgPaddingX = 16;
        int bgPaddingY = 10;
        int boxY = yBase - fm.getAscent() - bgPaddingY;
        int boxHeight = blockHeight + (bgPaddingY * 2);
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(
                sidePadding - bgPaddingX,
                boxY,
                maxTextWidth + (bgPaddingX * 2),
                boxHeight,
                18,
                18
        );

        for (int i = 0; i < styledLines.size(); i++) {
            StyledLine styledLine = styledLines.get(i);
            String line = styledLine.text();
            int lineWidth = fm.stringWidth(line);
            int x = Math.max(sidePadding, (width - lineWidth) / 2);
            int y = yBase + (i * lineHeight);

            // Draw black outline for readability on bright backgrounds.
            g2d.setColor(Color.BLACK);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    g2d.drawString(line, x + dx, y + dy);
                }
            }
            g2d.setColor(styledLine.color());
            g2d.drawString(line, x, y);
        }
    }

    private Font resolveFontForText(String text, int fontSize) {
        for (Font base : getBundledFonts()) {
            Font candidate = base.deriveFont(Font.BOLD, (float) fontSize);
            if (candidate.canDisplayUpTo(text) == -1) {
                return candidate;
            }
        }
        List<String> preferredFonts = List.of(
                "Myanmar Text",
                "Noto Sans Myanmar",
                "Pyidaungsu",
                "Arial Unicode MS",
                "Arial"
        );
        for (String family : preferredFonts) {
            Font candidate = new Font(family, Font.BOLD, fontSize);
            if (candidate.canDisplayUpTo(text) == -1) {
                return candidate;
            }
        }
        return new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
    }

    private List<Font> getBundledFonts() {
        List<Font> cached = bundledFonts;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (bundledFonts != null) {
                return bundledFonts;
            }
            List<Font> loaded = new ArrayList<>();
            try {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources("classpath*:fonts/*.ttf");
                for (Resource resource : resources) {
                    try (InputStream is = resource.getInputStream()) {
                        Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                        loaded.add(font);
                    } catch (Exception ex) {
                        log.warn("[ImageOverlay] Failed to load bundled font '{}': {}", resource.getFilename(), ex.getMessage());
                    }
                }
                if (!loaded.isEmpty()) {
                    log.info("[ImageOverlay] Loaded {} bundled font(s) from resources/fonts", loaded.size());
                }
            } catch (IOException ex) {
                log.warn("[ImageOverlay] Unable to scan bundled fonts: {}", ex.getMessage());
            }
            bundledFonts = Collections.unmodifiableList(loaded);
            return bundledFonts;
        }
    }

    private int resolveTextY(String textPosition, int height, FontMetrics fm, int blockHeight) {
        String pos = (textPosition == null || textPosition.isBlank())
                ? "BOTTOM"
                : textPosition.trim().toUpperCase();
        return switch (pos) {
            case "TOP" -> Math.max(fm.getAscent() + 24, 36);
            case "CENTER" -> (height - blockHeight) / 2 + fm.getAscent();
            default -> height - Math.max(30, fm.getDescent() + 28) - blockHeight + fm.getAscent();
        };
    }

    private List<String> wrapText(String text, FontMetrics fm, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }

        boolean hasSpaces = text.contains(" ");
        if (hasSpaces) {
            String[] words = text.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (fm.stringWidth(candidate) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                    }
                    current.setLength(0);
                    current.append(word);
                }
                if (lines.size() >= maxLines) {
                    break;
                }
            }
            if (!current.isEmpty() && lines.size() < maxLines) {
                lines.add(current.toString());
            }
        } else {
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                current.append(text.charAt(i));
                if (fm.stringWidth(current.toString()) > maxWidth) {
                    current.deleteCharAt(current.length() - 1);
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                    }
                    current.setLength(0);
                    current.append(text.charAt(i));
                }
                if (lines.size() >= maxLines) {
                    break;
                }
            }
            if (!current.isEmpty() && lines.size() < maxLines) {
                lines.add(current.toString());
            }
        }

        if (lines.isEmpty()) {
            lines.add(text);
        }
        if (lines.size() > maxLines) {
            return lines.subList(0, maxLines);
        }
        return lines;
    }

    private record StyledLine(String text, Color color) {}
}