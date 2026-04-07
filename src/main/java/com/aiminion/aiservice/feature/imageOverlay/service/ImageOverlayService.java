package com.aiminion.aiservice.feature.imageOverlay.service;

import com.aiminion.aiservice.config.ImageOverlayConfig;
import com.aiminion.aiservice.feature.imageOverlay.request.OverlayRequest;
import com.aiminion.aiservice.feature.imageOverlay.response.OverlayResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.Instant;

/**
 * Handles image composition — overlays logo and photo on a base image.
 * Positions/sizes are fully driven by ImageOverlayConfig.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageOverlayService {

    private final ImageOverlayConfig overlayConfig;

    public OverlayResult compose(OverlayRequest request) {
        try {
            log.info("[ImageOverlay] Composing image from URL='{}'", request.baseImageUrl());

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
                overlayImage(g2d, base, request.logoUrl(),
                        resolve(request.logoWidth(),  cfg.getWidth()),
                        resolve(request.logoHeight(), cfg.getHeight()),
                        resolve(request.logoMargin(), cfg.getMargin()),
                        position);
            }

            // ── Overlay photo ─────────────────────────────────────────────────
            if (request.photoUrl() != null && !request.photoUrl().isBlank()) {
                ImageOverlayConfig.Photo cfg = overlayConfig.getPhoto();  // ← Photo, not OverlayItem
                String position = resolve(request.photoPosition(), cfg.getPosition());
                log.info("[ImageOverlay] Overlaying photo at position={}", position);
                overlayImage(g2d, base, request.photoUrl(),
                        resolve(request.photoWidth(),  cfg.getWidth()),
                        resolve(request.photoHeight(), cfg.getHeight()),
                        resolve(request.photoMargin(), cfg.getMargin()),
                        position);
            }

            g2d.dispose();

            // ── Convert to bytes ──────────────────────────────────────────────
            byte[] imageBytes = toBytes(base);
            String imageName  = "composed_" + Instant.now().getEpochSecond() + ".png";

            log.info("[ImageOverlay] Composition complete — file={}", imageName);

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
        URL imageUrl = URI.create(url).toURL();
        HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(500000);
        conn.setReadTimeout(500000);

//        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new IOException("Failed to fetch image. HTTP Status: " + status);
        }

        try (InputStream is = conn.getInputStream()) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                throw new IOException("ImageIO.read returned null (invalid image format)");
            }
            return img;
        }
    }

    private byte[] toBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}