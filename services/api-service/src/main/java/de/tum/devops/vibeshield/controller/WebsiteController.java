package de.tum.devops.vibeshield.controller;

import de.tum.devops.vibeshield.generated.api.WebsitesApi;
import de.tum.devops.vibeshield.generated.model.CreateWebsiteRequest;
import de.tum.devops.vibeshield.generated.model.Website;
import de.tum.devops.vibeshield.mapper.WebsiteMapper;
import de.tum.devops.vibeshield.security.CurrentUser;
import de.tum.devops.vibeshield.service.WebsiteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Implements the generated Websites contract (api/openapi.yaml). All request/response
 * shapes come from the spec; this class only wires identity, service calls, and
 * status codes together.
 */
@RestController
public class WebsiteController implements WebsitesApi {

    private final WebsiteService websiteService;
    private final CurrentUser currentUser;

    public WebsiteController(WebsiteService websiteService, CurrentUser currentUser) {
        this.websiteService = websiteService;
        this.currentUser = currentUser;
    }

    @Override
    public ResponseEntity<Website> createWebsite(CreateWebsiteRequest createWebsiteRequest) {
        var website = websiteService.create(
                currentUser.require(), createWebsiteRequest.getUrl(), createWebsiteRequest.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(WebsiteMapper.toModel(website));
    }

    @Override
    public ResponseEntity<List<Website>> listWebsites() {
        List<Website> websites = websiteService.list(currentUser.require()).stream()
                .map(WebsiteMapper::toModel)
                .toList();
        return ResponseEntity.ok(websites);
    }
}
