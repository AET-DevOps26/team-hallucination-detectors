package de.tum.devops.vibeshield.scannerclient.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tum.devops.vibeshield.scannerclient.model.ScanCheck;
import de.tum.devops.vibeshield.scannerclient.model.ScannerFinding;
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
 * ScanExecutionResult
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ScanExecutionResult {

  /**
   * Outcome of this execution.
   */
  public enum StatusEnum {
    COMPLETED("Completed"),
    
    FAILED("Failed");

    private final String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private StatusEnum status;

  @Valid
  private List<ScanCheck> executedChecks = new ArrayList<>();

  @Valid
  private List<URI> pages = new ArrayList<>();

  @Valid
  private List<@Valid ScannerFinding> findings = new ArrayList<>();

  private @Nullable String errorMessage = null;

  public ScanExecutionResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScanExecutionResult(StatusEnum status, List<ScanCheck> executedChecks, List<URI> pages, List<@Valid ScannerFinding> findings) {
    this.status = status;
    this.executedChecks = executedChecks;
    this.pages = pages;
    this.findings = findings;
  }

  public ScanExecutionResult status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Outcome of this execution.
   * @return status
   */
  @NotNull 
  @Schema(name = "status", description = "Outcome of this execution.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public ScanExecutionResult executedChecks(List<ScanCheck> executedChecks) {
    this.executedChecks = executedChecks;
    return this;
  }

  public ScanExecutionResult addExecutedChecksItem(ScanCheck executedChecksItem) {
    if (this.executedChecks == null) {
      this.executedChecks = new ArrayList<>();
    }
    this.executedChecks.add(executedChecksItem);
    return this;
  }

  /**
   * Checks that were actually executed. May be a subset of the requested checks while some check types are not yet implemented — callers must not assume a requested check ran unless it appears here. 
   * @return executedChecks
   */
  @NotNull @Valid 
  @Schema(name = "executedChecks", description = "Checks that were actually executed. May be a subset of the requested checks while some check types are not yet implemented — callers must not assume a requested check ran unless it appears here. ", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("executedChecks")
  public List<ScanCheck> getExecutedChecks() {
    return executedChecks;
  }

  public void setExecutedChecks(List<ScanCheck> executedChecks) {
    this.executedChecks = executedChecks;
  }

  public ScanExecutionResult pages(List<URI> pages) {
    this.pages = pages;
    return this;
  }

  public ScanExecutionResult addPagesItem(URI pagesItem) {
    if (this.pages == null) {
      this.pages = new ArrayList<>();
    }
    this.pages.add(pagesItem);
    return this;
  }

  /**
   * URLs that were actually visited during the scan.
   * @return pages
   */
  @NotNull @Valid 
  @Schema(name = "pages", description = "URLs that were actually visited during the scan.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("pages")
  public List<URI> getPages() {
    return pages;
  }

  public void setPages(List<URI> pages) {
    this.pages = pages;
  }

  public ScanExecutionResult findings(List<@Valid ScannerFinding> findings) {
    this.findings = findings;
    return this;
  }

  public ScanExecutionResult addFindingsItem(ScannerFinding findingsItem) {
    if (this.findings == null) {
      this.findings = new ArrayList<>();
    }
    this.findings.add(findingsItem);
    return this;
  }

  /**
   * Discovered issues; empty when the site passed all checks or the scan failed.
   * @return findings
   */
  @NotNull @Valid 
  @Schema(name = "findings", description = "Discovered issues; empty when the site passed all checks or the scan failed.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("findings")
  public List<@Valid ScannerFinding> getFindings() {
    return findings;
  }

  public void setFindings(List<@Valid ScannerFinding> findings) {
    this.findings = findings;
  }

  public ScanExecutionResult errorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Why the scan failed; only set when `status` is `Failed`.
   * @return errorMessage
   */
  
  @Schema(name = "errorMessage", description = "Why the scan failed; only set when `status` is `Failed`.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorMessage")
  public @Nullable String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScanExecutionResult scanExecutionResult = (ScanExecutionResult) o;
    return Objects.equals(this.status, scanExecutionResult.status) &&
        Objects.equals(this.executedChecks, scanExecutionResult.executedChecks) &&
        Objects.equals(this.pages, scanExecutionResult.pages) &&
        Objects.equals(this.findings, scanExecutionResult.findings) &&
        Objects.equals(this.errorMessage, scanExecutionResult.errorMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, executedChecks, pages, findings, errorMessage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScanExecutionResult {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    executedChecks: ").append(toIndentedString(executedChecks)).append("\n");
    sb.append("    pages: ").append(toIndentedString(pages)).append("\n");
    sb.append("    findings: ").append(toIndentedString(findings)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
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

