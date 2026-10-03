package com.pmp.predictivemaintenance.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String chat(String message) {
        try {
            return chatClient
                    .prompt()
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("401") || errorMsg.contains("API_KEY_INVALID")) {
                    return "Error: Invalid Gemini API Key or authentication failed.";
                }
                if (errorMsg.contains("404") || errorMsg.contains("not found")) {
                    return "Error: The configured Gemini model is unavailable or no longer exists.";
                }
                if (errorMsg.contains("429") || errorMsg.contains("quota")) {
                    return "Error: Rate limit or quota exceeded for Gemini API.";
                }
            }
            return "Error: Communication with Gemini API failed. Please try again later.";
        }
    }
}