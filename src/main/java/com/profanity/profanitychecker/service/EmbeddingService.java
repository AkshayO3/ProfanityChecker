package com.profanity.profanitychecker.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface EmbeddingService {
    JsonNode generateEmbeddings(String text);

}
