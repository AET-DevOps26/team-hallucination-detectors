package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.exception.ConflictException;
import de.tum.devops.vibeshield.exception.ValidationException;
import de.tum.devops.vibeshield.model.Website;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for the registration rules: URL validation, name defaulting, duplicates. */
@ExtendWith(MockitoExtension.class)
class WebsiteServiceTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "dev@vibeshield.dev");

    @Mock
    private WebsiteRepository websiteRepository;

    @Test
    void create_persistsWebsiteWithOwnerAndExplicitName() {
        WebsiteService service = new WebsiteService(websiteRepository);
        when(websiteRepository.existsByOwnerIdAndUrl(7L, "https://shop.example.org")).thenReturn(false);
        when(websiteRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(USER, URI.create("https://shop.example.org"), "My shop");

        ArgumentCaptor<Website> saved = ArgumentCaptor.forClass(Website.class);
        verify(websiteRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(7L);
        assertThat(saved.getValue().getUrl()).isEqualTo("https://shop.example.org");
        assertThat(saved.getValue().getName()).isEqualTo("My shop");
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void create_defaultsNameToUrlHost() {
        WebsiteService service = new WebsiteService(websiteRepository);
        when(websiteRepository.existsByOwnerIdAndUrl(any(), any())).thenReturn(false);
        when(websiteRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Website website = service.create(USER, URI.create("https://shop.example.org/landing"), null);

        assertThat(website.getName()).isEqualTo("shop.example.org");
    }

    @Test
    void create_rejectsNonHttpSchemes() {
        WebsiteService service = new WebsiteService(websiteRepository);

        assertThatThrownBy(() -> service.create(USER, URI.create("ftp://shop.example.org"), null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsRelativeUrls() {
        WebsiteService service = new WebsiteService(websiteRepository);

        assertThatThrownBy(() -> service.create(USER, URI.create("shop.example.org"), null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsOverlongUrlsBeforeDatabaseInsert() {
        WebsiteService service = new WebsiteService(websiteRepository);
        String overlongUrl = "https://shop.example.org/" + "a".repeat(2049);

        assertThatThrownBy(() -> service.create(USER, URI.create(overlongUrl), null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("url must be at most 2048 characters");
    }

    @Test
    void create_rejectsOverlongNamesBeforeDatabaseInsert() {
        WebsiteService service = new WebsiteService(websiteRepository);
        when(websiteRepository.existsByOwnerIdAndUrl(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(USER, URI.create("https://shop.example.org"), "a".repeat(256)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("name must be at most 255 characters");
    }

    @Test
    void create_rejectsDuplicateUrlForSameUser() {
        WebsiteService service = new WebsiteService(websiteRepository);
        when(websiteRepository.existsByOwnerIdAndUrl(7L, "https://shop.example.org")).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER, URI.create("https://shop.example.org"), null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void create_mapsUniqueConstraintRaceToDuplicateConflict() {
        WebsiteService service = new WebsiteService(websiteRepository);
        when(websiteRepository.existsByOwnerIdAndUrl(7L, "https://shop.example.org")).thenReturn(false);
        when(websiteRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.create(USER, URI.create("https://shop.example.org"), null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }
}
