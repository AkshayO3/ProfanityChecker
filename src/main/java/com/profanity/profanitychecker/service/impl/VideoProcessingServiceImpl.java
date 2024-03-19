package com.profanity.profanitychecker.service.impl;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.*;
import com.profanity.profanitychecker.service.VideoProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VideoProcessingServiceImpl implements VideoProcessingService {

    @Override
    public List<String> processVideo(MultipartFile file) {
        Path tempFilePath = null;

        try {
            tempFilePath = saveUploadedFileTemporarily(file);

            // Extract frames to the filesystem
            Path framesDirectory = extractFrames(tempFilePath);

            // Convert extracted frames to Base64 strings
            List<String> base64Frames = convertFramesToBase64(framesDirectory);

            // Here you can do something with the base64 encoded frames
            System.out.println("Number of Base64 encoded frames: " + base64Frames.size());

            return base64Frames;

        } catch (IOException e) {
            throw new RuntimeException("Failed to process video", e);
        } finally {
            // Cleanup: Delete the temporary file and frames
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

    private Path extractFrames(Path videoFilePath) throws IOException {
        Path outputDir = Files.createTempDirectory("frames");

        String outputPattern = outputDir.resolve("frame_%03d.png").toString();

        FFmpeg.atPath()
                .addInput(com.github.kokorin.jaffree.ffmpeg.UrlInput.fromPath(videoFilePath))
                .addOutput(UrlOutput.toUrl(outputPattern)
                        .setFrameRate(1) // Extract 1 frame per second as an example
                        .setFrameCount(StreamType.VIDEO,10L)) // Limit to 10 frames for example
                .setProgressListener(new ProgressListener() {
                    @Override
                    public void onProgress(FFmpegProgress progress) {
                        System.out.println("Frame: " + progress.getFrame());
                        // Additional progress information can be logged if necessary
                    }
                })
                .execute();

        return outputDir;
    }

    private List<String> convertFramesToBase64(Path framesDirectory) throws IOException {
        File[] frameFiles = framesDirectory.toFile().listFiles();
        List<String> base64Frames = new ArrayList<>();

        if (frameFiles != null) {
            for (File frame : frameFiles) {
                // Read the image file into a BufferedImage
                BufferedImage img = ImageIO.read(frame);

                // Convert BufferedImage to ByteArrayOutputStream
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);

                // Convert ByteArrayOutputStream to byte array
                byte[] imageBytes = baos.toByteArray();

                // Encode byte array to Base64 string
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                base64Frames.add(base64Image);

                // Cleanup: Delete the frame file if no longer needed
                frame.delete();
            }
        }

        // Optional: Delete the frames directory if it is now empty
        try {
            Files.delete(framesDirectory);
        } catch (IOException e) {
            // Handle the possibility that the directory is not empty or other IO issues
            e.printStackTrace();
        }

        return base64Frames;
    }

    private void deleteTemporaryFile(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
