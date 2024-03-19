package com.profanity.profanitychecker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VideoAnalysisResponse {
    private String description;
    private List<AnalysisResult.ModerationResult> moderationResults;
}
