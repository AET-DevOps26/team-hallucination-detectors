package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
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
 * Configuration for a scan run. All fields are optional.
 */

@Schema(name = "ScanRequest", description = "Configuration for a scan run. All fields are optional.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ScanRequest {

  @Valid
  private List<ScanCheck> checks = new ArrayList<>();

  private @Nullable Integer crawlDepth;

  private @Nullable Boolean includeSubdomains;

  public ScanRequest checks(List<ScanCheck> checks) {
    this.checks = checks;
    return this;
  }

  public ScanRequest addChecksItem(ScanCheck checksItem) {
    if (this.checks == null) {
      this.checks = new ArrayList<>();
    }
    this.checks.add(checksItem);
    return this;
  }

  /**
   * Checks to run; defaults to all available checks.
   * @return checks
   */
  @Valid 
  @Schema(name = "checks", description = "Checks to run; defaults to all available checks.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("checks")
  public List<ScanCheck> getChecks() {
    return checks;
  }

  public void setChecks(List<ScanCheck> checks) {
    this.checks = checks;
  }

  public ScanRequest crawlDepth(@Nullable Integer crawlDepth) {
    this.crawlDepth = crawlDepth;
    return this;
  }

  /**
   * How many link levels to follow from the start URL. Defaults to 0, which scans only the registered URL.
   * minimum: 0
   * maximum: 3
   * @return crawlDepth
   */
  @Min(0) @Max(3) 
  @Schema(name = "crawlDepth", description = "How many link levels to follow from the start URL. Defaults to 0, which scans only the registered URL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("crawlDepth")
  public @Nullable Integer getCrawlDepth() {
    return crawlDepth;
  }

  public void setCrawlDepth(@Nullable Integer crawlDepth) {
    this.crawlDepth = crawlDepth;
  }

  public ScanRequest includeSubdomains(@Nullable Boolean includeSubdomains) {
    this.includeSubdomains = includeSubdomains;
    return this;
  }

  /**
   * Whether the crawl may follow links to subdomains of the registered host. Defaults to false.
   * @return includeSubdomains
   */
  
  @Schema(name = "includeSubdomains", description = "Whether the crawl may follow links to subdomains of the registered host. Defaults to false.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("includeSubdomains")
  public @Nullable Boolean getIncludeSubdomains() {
    return includeSubdomains;
  }

  public void setIncludeSubdomains(@Nullable Boolean includeSubdomains) {
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
    ScanRequest scanRequest = (ScanRequest) o;
    return Objects.equals(this.checks, scanRequest.checks) &&
        Objects.equals(this.crawlDepth, scanRequest.crawlDepth) &&
        Objects.equals(this.includeSubdomains, scanRequest.includeSubdomains);
  }

  @Override
  public int hashCode() {
    return Objects.hash(checks, crawlDepth, includeSubdomains);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScanRequest {\n");
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

