package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.net.URI;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateWebsiteRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class CreateWebsiteRequest {

  private URI url;

  private @Nullable String name;

  public CreateWebsiteRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateWebsiteRequest(URI url) {
    this.url = url;
  }

  public CreateWebsiteRequest url(URI url) {
    this.url = url;
    return this;
  }

  /**
   * Must be an absolute http(s) URL.
   * @return url
   */
  @NotNull @Valid 
  @Schema(name = "url", example = "https://my-vibecoded-shop.lovable.app", description = "Must be an absolute http(s) URL.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("url")
  public URI getUrl() {
    return url;
  }

  public void setUrl(URI url) {
    this.url = url;
  }

  public CreateWebsiteRequest name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Optional display name; defaults to the URL host.
   * @return name
   */
  @Size(max = 255) 
  @Schema(name = "name", description = "Optional display name; defaults to the URL host.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateWebsiteRequest createWebsiteRequest = (CreateWebsiteRequest) o;
    return Objects.equals(this.url, createWebsiteRequest.url) &&
        Objects.equals(this.name, createWebsiteRequest.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateWebsiteRequest {\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

