package com.aiminion.aiservice.common.util;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIStyles {

    public static List<String> getTranslateStyles(){
        return List.of(
                "natural",
                "friendly",
                "professional",
                "formal",
                "casual",
                "polite",
                "serious",
                "cheerful",
                "sad",
                "angry",
                "excited",
                "calm",
                "relaxed",
                "confident",
                "authoritative"
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
}
