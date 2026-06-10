package de.tum.devops.vibeshield.scanner.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ScanExecutionRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ScanExecutionRequest {

  private URI url;

  @Valid
  private List<ScanCheck> checks = new ArrayList<>();

  private Integer crawlDepth = 0;

  private Boolean includeSubdomains = false;

  public ScanExecutionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScanExecutionRequest(URI url, List<ScanCheck> checks) {
    this.url = url;
    this.checks = checks;
  }

  public ScanExecutionRequest url(URI url) {
    this.url = url;
    return this;
  }

  /**
   * Absolute http(s) URL of the site to scan.
   * @return url
   */
  @NotNull @Valid 
  @Schema(name = "url", example = "https://my-vibecoded-shop.lovable.app", description = "Absolute http(s) URL of the site to scan.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("url")
  public URI getUrl() {
    return url;
  }

  public void setUrl(URI url) {
    this.url = url;
  }

  public ScanExecutionRequest checks(List<ScanCheck> checks) {
    this.checks = checks;
    return this;
  }

  public ScanExecutionRequest addChecksItem(ScanCheck checksItem) {
    if (this.checks == null) {
      this.checks = new ArrayList<>();
    }
    this.checks.add(checksItem);
    return this;
  }

  /**
   * Which checks to run in this execution.
   * @return checks
   */
  @NotNull @Valid @Size(min = 1) 
  @Schema(name = "checks", description = "Which checks to run in this execution.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("checks")
  public List<ScanCheck> getChecks() {
    return checks;
  }

  public void setChecks(List<ScanCheck> checks) {
    this.checks = checks;
  }

  public ScanExecutionRequest crawlDepth(Integer crawlDepth) {
    this.crawlDepth = crawlDepth;
    return this;
  }

  /**
   * Link levels to follow from the start URL. 0 scans only the start URL.
   * minimum: 0
   * maximum: 3
   * @return crawlDepth
   */
  @Min(0) @Max(3) 
  @Schema(name = "crawlDepth", description = "Link levels to follow from the start URL. 0 scans only the start URL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("crawlDepth")
  public Integer getCrawlDepth() {
    return crawlDepth;
  }

  public void setCrawlDepth(Integer crawlDepth) {
    this.crawlDepth = crawlDepth;
  }

  public ScanExecutionRequest includeSubdomains(Boolean includeSubdomains) {
    this.includeSubdomains = includeSubdomains;
    return this;
  }

  /**
   * Whether the crawl may follow links to subdomains of the start host.
   * @return includeSubdomains
   */
  
  @Schema(name = "includeSubdomains", description = "Whether the crawl may follow links to subdomains of the start host.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("includeSubdomains")
  public Boolean getIncludeSubdomains() {
    return includeSubdomains;
  }

  public void setIncludeSubdomains(Boolean includeSubdomains) {
    this.includeSubdomains = includeSubdomains;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScanExecutionRequest scanExecutionRequest = (ScanExecutionRequest) o;
    return Objects.equals(this.url, scanExecutionRequest.url) &&
        Objects.equals(this.checks, scanExecutionRequest.checks) &&
        Objects.equals(this.crawlDepth, scanExecutionRequest.crawlDepth) &&
        Objects.equals(this.includeSubdomains, scanExecutionRequest.includeSubdomains);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, checks, crawlDepth, includeSubdomains);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScanExecutionRequest {\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    checks: ").append(toIndentedString(checks)).append("\n");
    sb.append("    crawlDepth: ").append(toIndentedString(crawlDepth)).append("\n");
    sb.append("    includeSubdomains: ").append(toIndentedString(includeSubdomains)).append("\n");
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

