package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.project.application.usecase.PublishedScreenshot;

import java.util.List;

/**
 * Capture d'un projet : image publique ({@code url}, {@code width}, {@code height}, {@code altText}) et
 * légende ({@code null} si aucune).
 */
public record ProjectScreenshotResponse(PublicImage image, String caption) {

    static List<ProjectScreenshotResponse> from(List<PublishedScreenshot> screenshots) {
        return screenshots.stream()
            .map(screenshot -> new ProjectScreenshotResponse(screenshot.image(), screenshot.caption()))
            .toList();
    }
}
