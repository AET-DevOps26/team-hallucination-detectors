package de.tum.devops.vibeshield.mapper;

import de.tum.devops.vibeshield.model.Website;

import java.net.URI;
import java.time.ZoneOffset;

/** Converts persistence entities into the response models generated from the contract. */
public final class WebsiteMapper {

    private WebsiteMapper() {
    }

    public static de.tum.devops.vibeshield.generated.model.Website toModel(Website entity) {
        return new de.tum.devops.vibeshield.generated.model.Website()
                .id(entity.getId())
                .url(URI.create(entity.getUrl()))
                .name(entity.getName())
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
