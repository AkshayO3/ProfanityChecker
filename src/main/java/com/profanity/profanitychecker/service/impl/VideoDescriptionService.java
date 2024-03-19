package com.profanity.profanitychecker.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class VideoDescriptionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Autowired
    public VideoDescriptionService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateVideoDescription(List<String> base64Frames) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        ObjectNode payload = constructPayload(base64Frames);

        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
        String response = restTemplate.postForObject("https://api.openai.com/v1/chat/completions", entity, String.class);

        return parseDescriptionFromResponse(response);
    }

    private ObjectNode constructPayload(List<String> base64Frames) {

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", "gpt-4-vision-preview"); // Adjust model as needed
        ArrayNode messages = payload.putArray("messages");

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        ArrayNode content = userMessage.putArray("content");

        // Adding a text message (optional, depending on your prompt requirements)
        ObjectNode textMessage = content.addObject();
        textMessage.put("type", "text");
        textMessage.put("text", "Provide a detailed description for these video frames.");

        // Assuming you're including every Nth frame to avoid overloading the request
        int frameSamplingRate = 50; // Example: Adjust based on your actual needs

        for (int i = 0; i < base64Frames.size(); i += frameSamplingRate) {

            ObjectNode base64Image = content.addObject();
            base64Image.put("type", "image_url");

            ObjectNode imageUrl = base64Image.putObject("image_url");
            imageUrl.put("url", "data:image/jpeg;base64," + base64Frames.get(i));
        }

        payload.put("max_tokens", 300); // Adjust based on your needs
        return payload;
    }


    private String parseDescriptionFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (!choices.isEmpty()) {
                // Extracting the message content from the first choice
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.path("message").path("content");
                if (!message.isMissingNode()) {
                    return message.asText().trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing the OpenAI API response";
        }
        return "No description found in the response";
    }


}
