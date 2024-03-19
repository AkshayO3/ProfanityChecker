package com.profanity.profanitychecker.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoProcessingService {
    List<String> processVideo(MultipartFile file);
}
