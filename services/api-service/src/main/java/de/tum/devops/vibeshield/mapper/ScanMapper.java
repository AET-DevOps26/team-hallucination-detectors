package de.tum.devops.vibeshield.mapper;

import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.model.Scan;

import java.time.ZoneOffset;

/** Converts scan/finding entities into the response models generated from the contract. */
public final class ScanMapper {

    private ScanMapper() {
    }

    public static de.tum.devops.vibeshield.generated.model.Scan toModel(Scan entity, Integer findingCount) {
        return new de.tum.devops.vibeshield.generated.model.Scan()
                .id(entity.getId())
                .websiteId(entity.getWebsiteId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .completedAt(entity.getCompletedAt() == null
                        ? null : entity.getCompletedAt().atOffset(ZoneOffset.UTC))
                .errorMessage(entity.getErrorMessage())
                .findingCount(findingCount);
    }

    public static de.tum.devops.vibeshield.generated.model.Finding toModel(Finding entity) {
        return new de.tum.devops.vibeshield.generated.model.Finding()
                .id(entity.getId())
                .scanId(entity.getScanId())
                .check(entity.getCheckType())
                .title(entity.getTitle())
                .severity(entity.getSeverity())
                .affected(entity.getAffected())
                .explanation(entity.getExplanation())
                .suggestedFix(entity.getSuggestedFix())
                .status(entity.getStatus());
    }
}
