package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import de.tum.devops.vibeshield.generated.model.ComparisonFinding;
import de.tum.devops.vibeshield.generated.model.ScanComparisonSummary;
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
 * ScanComparison
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ScanComparison {

  private Long scanId;

  private @Nullable Long previousScanId = null;

  private Boolean comparable;

  private String message;

  private ScanComparisonSummary summary;

  @Valid
  private List<@Valid ComparisonFinding> findings = new ArrayList<>();

  @Valid
  private List<@Valid ComparisonFinding> actionPlan = new ArrayList<>();

  public ScanComparison() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScanComparison(Long scanId, Boolean comparable, String message, ScanComparisonSummary summary, List<@Valid ComparisonFinding> findings, List<@Valid ComparisonFinding> actionPlan) {
    this.scanId = scanId;
    this.comparable = comparable;
    this.message = message;
    this.summary = summary;
    this.findings = findings;
    this.actionPlan = actionPlan;
  }

  public ScanComparison scanId(Long scanId) {
    this.scanId = scanId;
    return this;
  }

  /**
   * Get scanId
   * @return scanId
   */
  @NotNull 
  @Schema(name = "scanId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("scanId")
  public Long getScanId() {
    return scanId;
  }

  public void setScanId(Long scanId) {
    this.scanId = scanId;
  }

  public ScanComparison previousScanId(@Nullable Long previousScanId) {
    this.previousScanId = previousScanId;
    return this;
  }

  /**
   * Get previousScanId
   * @return previousScanId
   */
  
  @Schema(name = "previousScanId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("previousScanId")
  public @Nullable Long getPreviousScanId() {
    return previousScanId;
  }

  public void setPreviousScanId(@Nullable Long previousScanId) {
    this.previousScanId = previousScanId;
  }

  public ScanComparison comparable(Boolean comparable) {
    this.comparable = comparable;
    return this;
  }

  /**
   * False when the scan is not completed or no previous completed scan exists.
   * @return comparable
   */
  @NotNull 
  @Schema(name = "comparable", description = "False when the scan is not completed or no previous completed scan exists.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("comparable")
  public Boolean getComparable() {
    return comparable;
  }

  public void setComparable(Boolean comparable) {
    this.comparable = comparable;
  }

  public ScanComparison message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @NotNull 
  @Schema(name = "message", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ScanComparison summary(ScanComparisonSummary summary) {
    this.summary = summary;
    return this;
  }

  /**
   * Get summary
   * @return summary
   */
  @NotNull @Valid 
  @Schema(name = "summary", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("summary")
  public ScanComparisonSummary getSummary() {
    return summary;
  }

  public void setSummary(ScanComparisonSummary summary) {
    this.summary = summary;
  }

  public ScanComparison findings(List<@Valid ComparisonFinding> findings) {
    this.findings = findings;
    return this;
  }

  public ScanComparison addFindingsItem(ComparisonFinding findingsItem) {
    if (this.findings == null) {
      this.findings = new ArrayList<>();
    }
    this.findings.add(findingsItem);
    return this;
  }

  /**
   * Fixed, still-present, and newly introduced findings.
   * @return findings
   */
  @NotNull @Valid 
  @Schema(name = "findings", description = "Fixed, still-present, and newly introduced findings.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("findings")
  public List<@Valid ComparisonFinding> getFindings() {
    return findings;
  }

  public void setFindings(List<@Valid ComparisonFinding> findings) {
    this.findings = findings;
  }

  public ScanComparison actionPlan(List<@Valid ComparisonFinding> actionPlan) {
    this.actionPlan = actionPlan;
    return this;
  }

  public ScanComparison addActionPlanItem(ComparisonFinding actionPlanItem) {
    if (this.actionPlan == null) {
      this.actionPlan = new ArrayList<>();
    }
    this.actionPlan.add(actionPlanItem);
    return this;
  }

  /**
   * Current open findings ordered by severity and rough effort estimate.
   * @return actionPlan
   */
  @NotNull @Valid 
  @Schema(name = "actionPlan", description = "Current open findings ordered by severity and rough effort estimate.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("actionPlan")
  public List<@Valid ComparisonFinding> getActionPlan() {
    return actionPlan;
  }

  public void setActionPlan(List<@Valid ComparisonFinding> actionPlan) {
    this.actionPlan = actionPlan;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScanComparison scanComparison = (ScanComparison) o;
    return Objects.equals(this.scanId, scanComparison.scanId) &&
        Objects.equals(this.previousScanId, scanComparison.previousScanId) &&
        Objects.equals(this.comparable, scanComparison.comparable) &&
        Objects.equals(this.message, scanComparison.message) &&
        Objects.equals(this.summary, scanComparison.summary) &&
        Objects.equals(this.findings, scanComparison.findings) &&
        Objects.equals(this.actionPlan, scanComparison.actionPlan);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scanId, previousScanId, comparable, message, summary, findings, actionPlan);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScanComparison {\n");
    sb.append("    scanId: ").append(toIndentedString(scanId)).append("\n");
    sb.append("    previousScanId: ").append(toIndentedString(previousScanId)).append("\n");
    sb.append("    comparable: ").append(toIndentedString(comparable)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
    sb.append("    findings: ").append(toIndentedString(findings)).append("\n");
    sb.append("    actionPlan: ").append(toIndentedString(actionPlan)).append("\n");
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

