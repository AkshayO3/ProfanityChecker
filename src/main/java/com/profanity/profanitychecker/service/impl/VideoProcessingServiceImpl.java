package com.profanity.profanitychecker.service.impl;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FrameOutput;
import com.profanity.profanitychecker.service.VideoProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VideoProcessingServiceImpl implements VideoProcessingService {

    @Override
    public void processVideo(MultipartFile file) {
        Path tempFilePath = null;
        try {
            // Save the uploaded file temporarily
            tempFilePath = saveUploadedFileTemporarily(file);
            // Extract and process frames
            extractFrames(tempFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process video", e);
        } finally {
            // Cleanup: Delete the temporary file
            if (tempFilePath != null) {
                deleteTemporaryFile(tempFilePath);
            }
        }
    }

    private Path saveUploadedFileTemporarily(MultipartFile file) throws IOException {
        Path tempFile = Files.createTempFile("upload_", ".tmp");
        file.transferTo(tempFile.toFile());
        return tempFile;
    }

    private void extractFrames(Path videoFilePath) {
        Path outputDir = videoFilePath.getParent().resolve("frames");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create output directory for frames", e);
        }

        String outputPattern = outputDir.resolve("frame_%03d.png").toString();

        FFmpeg.atPath()
                .addInput(com.github.kokorin.jaffree.ffmpeg.UrlInput.fromPath(videoFilePath))
                .addOutput(com.github.kokorin.jaffree.ffmpeg.UrlOutput.toUrl(outputPattern)
                        .setFrameRate(1) // Extract 1 frame per second as an example
                        .setFrameCount(StreamType.VIDEO, 10L)) // Limit to 10 frames for example
                .execute();
    }

    private void deleteTemporaryFile(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
