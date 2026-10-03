package com.pmp.predictivemaintenance.ai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AiServiceManualIT {

    @Autowired
    private AiService aiService;

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    public void testGeminiChat_whenApiKeyIsPresent() {
        // This test will ONLY run if GEMINI_API_KEY is configured in the environment.
        // It provides a safe way to manually test Gemini integration without breaking 'mvn test' in CI/CD.

        String response = aiService.chat("Hello, this is a test. Reply with 'ACKNOWLEDGED'.");
        
        assertThat(response).isNotNull();
        assertThat(response).doesNotContain("Error:");
        
        System.out.println("Gemini API Response: " + response);
    }
}
