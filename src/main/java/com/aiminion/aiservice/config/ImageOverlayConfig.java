package com.aiminion.aiservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Controls all overlay positions and sizes.
 * Change positions/sizes here — zero code changes needed elsewhere.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "image.overlay")
public class ImageOverlayConfig {

    private Logo  logo  = new Logo();
    private Photo photo = new Photo();

    @Getter @Setter
    public static class Logo {
        private String position = "TOP_RIGHT";  // TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT, CENTER
        private int    width    = 120;
        private int    height   = 120;
        private int    margin   = 20;           // padding from edge in pixels
    }

    @Getter @Setter
    public static class Photo {
        private String position = "BOTTOM_LEFT";
        private int    width    = 150;
        private int    height   = 150;
        private int    margin   = 20;
    }
}
