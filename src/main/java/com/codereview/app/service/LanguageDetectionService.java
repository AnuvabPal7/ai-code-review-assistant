package com.codereview.app.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LanguageDetectionService {

    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.of(
            "java", "Java",
            "py", "Python",
            "js", "JavaScript",
            "ts", "TypeScript",
            "cpp", "C++",
            "c", "C",
            "cs", "C#",
            "go", "Go",
            "rb", "Ruby",
            "php", "PHP"
    );

    private static final String SUPPORTED_LANGUAGE = "Java";

    public String detectLanguage(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "Unknown";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return EXTENSION_TO_LANGUAGE.getOrDefault(extension, "Unknown");
    }

    public boolean isSupported(String fileName) {
        return SUPPORTED_LANGUAGE.equals(detectLanguage(fileName));
    }
}