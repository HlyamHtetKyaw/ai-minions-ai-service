package com.aiminion.aiservice.common.util;

import com.aiminion.aiservice.feature.response.VoiceModelDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIStyles {

    public static List<String> getTranslateStyles(){
        return List.of(
                "casual_social_media",
                "polite_educational",
                "formal_corporate",
                "youthful_trendy"
        );
    }

    public static List<String> voiceOverStyles(){
        return List.of(
                "male",
                "female",
                "boy",
                "girl",
                "young",
                "adult",
                "elderly",

                "calm",
                "energetic",
                "serious",
                "cheerful",
                "natural",
                "professional"
        );
    }

    /**
     * Gemini prebuilt TTS voices with style labels (Google naming). {@code id} is the API voiceName (lowercase).
     */
    public static final List<VoiceModelDescriptor> GEMINI_VOICE_CATALOG = List.of(
            new VoiceModelDescriptor("zephyr", "Bright"),
            new VoiceModelDescriptor("puck", "Upbeat"),
            new VoiceModelDescriptor("charon", "Informative"),
            new VoiceModelDescriptor("kore", "Firm"),
            new VoiceModelDescriptor("fenrir", "Excitable"),
            new VoiceModelDescriptor("leda", "Youthful"),
            new VoiceModelDescriptor("orus", "Firm"),
            new VoiceModelDescriptor("aoede", "Breezy"),
            new VoiceModelDescriptor("callirrhoe", "Easy-going"),
            new VoiceModelDescriptor("autonoe", "Bright"),
            new VoiceModelDescriptor("enceladus", "Breathy"),
            new VoiceModelDescriptor("iapetus", "Clear"),
            new VoiceModelDescriptor("umbriel", "Easy-going"),
            new VoiceModelDescriptor("algieba", "Smooth"),
            new VoiceModelDescriptor("despina", "Smooth"),
            new VoiceModelDescriptor("erinome", "Clear"),
            new VoiceModelDescriptor("algenib", "Gravelly"),
            new VoiceModelDescriptor("rasalgethi", "Informative"),
            new VoiceModelDescriptor("laomedeia", "Upbeat"),
            new VoiceModelDescriptor("achernar", "Soft"),
            new VoiceModelDescriptor("alnilam", "Firm"),
            new VoiceModelDescriptor("schedar", "Even"),
            new VoiceModelDescriptor("gacrux", "Mature"),
            new VoiceModelDescriptor("pulcherrima", "Forward"),
            new VoiceModelDescriptor("achird", "Friendly"),
            new VoiceModelDescriptor("zubenelgenubi", "Casual"),
            new VoiceModelDescriptor("vindemiatrix", "Gentle"),
            new VoiceModelDescriptor("sadachbia", "Lively"),
            new VoiceModelDescriptor("sadaltager", "Knowledgeable"),
            new VoiceModelDescriptor("sulafat", "Warm")
    );
}
