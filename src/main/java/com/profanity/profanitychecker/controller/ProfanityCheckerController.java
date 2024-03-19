package com.profanity.profanitychecker.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.profanity.profanitychecker.model.AnalysisResult;
import com.profanity.profanitychecker.service.*;
import com.profanity.profanitychecker.service.impl.VideoDescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profanity-check")
public class ProfanityCheckerController {

    private final FileProcessingService fileProcessingService;
    private final ProfanityAnalysisService profanityAnalysisService;
    private final URLContentFetcherService urlContentFetcherService;
    private final VideoProcessingService videoProcessingService;
    private final VideoDescriptionService videoDescriptionService;
    private final EmbeddingService embeddingService;

    public ProfanityCheckerController(FileProcessingService fileProcessingService, ProfanityAnalysisService profanityAnalysisService, URLContentFetcherService urlContentFetcherService, VideoProcessingService videoProcessingService, VideoDescriptionService videoDescriptionService, EmbeddingService embeddingService) {

        this.fileProcessingService = fileProcessingService;
        this.profanityAnalysisService = profanityAnalysisService;
        this.urlContentFetcherService = urlContentFetcherService;
        this.videoProcessingService = videoProcessingService;
        this.videoDescriptionService = videoDescriptionService;

        this.embeddingService = embeddingService;
    }

    // this method is for pdf and word files
    @PostMapping("/check-file")
    public ResponseEntity<String> checkFileForProfanity(@RequestParam("file") MultipartFile file) {
        try {
            String text = fileProcessingService.extractTextFromFile(file);
            AnalysisResult analysisResult = profanityAnalysisService.analyzeText(text);
            String summary = profanityAnalysisService.generateSummaryFromModerationResult(analysisResult);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing the file: " + e.getMessage());
        }
    }

    @PostMapping("/check-url")
    public ResponseEntity<String> checkUrlForProfanity(@RequestParam("url") String url) {
        try {
            String text = urlContentFetcherService.fetchContentFromUrl(url);
            AnalysisResult analysisResult = profanityAnalysisService.analyzeText(text);
            String summary = profanityAnalysisService.generateSummaryFromModerationResult(analysisResult);
            System.out.println("HERE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            return ResponseEntity.ok(summary);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing the URI: " + e.getMessage());
        }

    }

    @PostMapping("/check-audio")
    public ResponseEntity<String> checkAudioForProfanity(@RequestParam("audioFile")MultipartFile audioFile){
        try {
            AnalysisResult analysisResult = profanityAnalysisService.analyzeAudioFile(audioFile);
            String summary = profanityAnalysisService.generateSummaryFromModerationResult(analysisResult);
            System.out.println("IN AUDIO METHOD!!!!");
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing the audio file: " + e.getMessage());
        }
    }

    @PostMapping("/check-video")
    public ResponseEntity<String> checkVideoForProfanity(@RequestParam("videoFile") MultipartFile videoFile) {
        try {
            // Process the video to extract frames and convert them to base64 strings
            List<String> base64Frames = videoProcessingService.processVideo(videoFile);

            // Generate a description of the video from the base64 frames
            String description = videoDescriptionService.generateVideoDescription(base64Frames);
            System.out.println(description);

            // Check if the description is the specific message indicating inability to process
            if ("I'm sorry, I can't provide assistance with that request.".equals(description)) {
                // Modify the description to indicate potential explicit content
                description = "This content contains explicit and inappropriate material like blood and gore.";
            } else if ("Sorry, but I can't provide information about this image.".equals(description)) {
                description = "This content contains explicit and inappropriate material like blood and gore.";
            } else if ("Sorry, I can't provide that information.".equals(description)) {
                description = "This content contains explicit and inappropriate material like blood and gore.";
            } else if ("Sorry, I can't provide assistance with that request.".equals(description)) {
                description = "This content contains explicit and inappropriate material like blood and gore.";
            }

            // Analyze the potentially modified description for profanity
            AnalysisResult analysisResult = profanityAnalysisService.analyzeText(description);

            // Generate a summary from the analysis result
            String summary = profanityAnalysisService.generateSummaryFromModerationResult(analysisResult);

            System.out.println(summary);

            // Return the summary
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing the video file: " + e.getMessage());
        }
    }

    @PostMapping("/process-pdf")
    public ResponseEntity<?> processPdf(@RequestParam("file") MultipartFile file) {
        try {

            String text = fileProcessingService.extractTextFromFile(file);
            JsonNode embeddings = embeddingService.generateEmbeddings(text);

            return ResponseEntity.ok(embeddings);
        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing the PDF file: " + e.getMessage());
        }
    }
}
