package com.aiminion.aiservice.common.util;

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
}
