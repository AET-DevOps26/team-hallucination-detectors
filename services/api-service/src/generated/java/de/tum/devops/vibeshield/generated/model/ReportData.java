package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tum.devops.vibeshield.generated.model.ExecutiveSummary;
import de.tum.devops.vibeshield.generated.model.LaunchChecklist;
import de.tum.devops.vibeshield.generated.model.ReportFinding;
import de.tum.devops.vibeshield.generated.model.ReportSite;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ReportData
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ReportData {

  private Long scanId;

  private ReportSite site;

  private ScanStatus scanStatus;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime scanCreatedAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime scanCompletedAt = null;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime generatedAt;

  private LaunchChecklist safeToLaunch;

  private ExecutiveSummary executiveSummary;

  @Valid
  private List<@Valid ReportFinding> findings = new ArrayList<>();

  public ReportData() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReportData(Long scanId, ReportSite site, ScanStatus scanStatus, OffsetDateTime scanCreatedAt, OffsetDateTime generatedAt, LaunchChecklist safeToLaunch, ExecutiveSummary executiveSummary, List<@Valid ReportFinding> findings) {
    this.scanId = scanId;
    this.site = site;
    this.scanStatus = scanStatus;
    this.scanCreatedAt = scanCreatedAt;
    this.generatedAt = generatedAt;
    this.safeToLaunch = safeToLaunch;
    this.executiveSummary = executiveSummary;
    this.findings = findings;
  }

  public ReportData scanId(Long scanId) {
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

  public ReportData site(ReportSite site) {
    this.site = site;
    return this;
  }

  /**
   * Get site
   * @return site
   */
  @NotNull @Valid 
  @Schema(name = "site", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("site")
  public ReportSite getSite() {
    return site;
  }

  public void setSite(ReportSite site) {
    this.site = site;
  }

  public ReportData scanStatus(ScanStatus scanStatus) {
    this.scanStatus = scanStatus;
    return this;
  }

  /**
   * Get scanStatus
   * @return scanStatus
   */
  @NotNull @Valid 
  @Schema(name = "scanStatus", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("scanStatus")
  public ScanStatus getScanStatus() {
    return scanStatus;
  }

  public void setScanStatus(ScanStatus scanStatus) {
    this.scanStatus = scanStatus;
  }

  public ReportData scanCreatedAt(OffsetDateTime scanCreatedAt) {
    this.scanCreatedAt = scanCreatedAt;
    return this;
  }

  /**
   * Get scanCreatedAt
   * @return scanCreatedAt
   */
  @NotNull @Valid 
  @Schema(name = "scanCreatedAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("scanCreatedAt")
  public OffsetDateTime getScanCreatedAt() {
    return scanCreatedAt;
  }

  public void setScanCreatedAt(OffsetDateTime scanCreatedAt) {
    this.scanCreatedAt = scanCreatedAt;
  }

  public ReportData scanCompletedAt(@Nullable OffsetDateTime scanCompletedAt) {
    this.scanCompletedAt = scanCompletedAt;
    return this;
  }

  /**
   * Get scanCompletedAt
   * @return scanCompletedAt
   */
  @Valid 
  @Schema(name = "scanCompletedAt", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("scanCompletedAt")
  public @Nullable OffsetDateTime getScanCompletedAt() {
    return scanCompletedAt;
  }

  public void setScanCompletedAt(@Nullable OffsetDateTime scanCompletedAt) {
    this.scanCompletedAt = scanCompletedAt;
  }

  public ReportData generatedAt(OffsetDateTime generatedAt) {
    this.generatedAt = generatedAt;
    return this;
  }

  /**
   * Get generatedAt
   * @return generatedAt
   */
  @NotNull @Valid 
  @Schema(name = "generatedAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("generatedAt")
  public OffsetDateTime getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(OffsetDateTime generatedAt) {
    this.generatedAt = generatedAt;
  }

  public ReportData safeToLaunch(LaunchChecklist safeToLaunch) {
    this.safeToLaunch = safeToLaunch;
    return this;
  }

  /**
   * Get safeToLaunch
   * @return safeToLaunch
   */
  @NotNull @Valid 
  @Schema(name = "safeToLaunch", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("safeToLaunch")
  public LaunchChecklist getSafeToLaunch() {
    return safeToLaunch;
  }

  public void setSafeToLaunch(LaunchChecklist safeToLaunch) {
    this.safeToLaunch = safeToLaunch;
  }

  public ReportData executiveSummary(ExecutiveSummary executiveSummary) {
    this.executiveSummary = executiveSummary;
    return this;
  }

  /**
   * Get executiveSummary
   * @return executiveSummary
   */
  @NotNull @Valid 
  @Schema(name = "executiveSummary", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("executiveSummary")
  public ExecutiveSummary getExecutiveSummary() {
    return executiveSummary;
  }

  public void setExecutiveSummary(ExecutiveSummary executiveSummary) {
    this.executiveSummary = executiveSummary;
  }

  public ReportData findings(List<@Valid ReportFinding> findings) {
    this.findings = findings;
    return this;
  }

  public ReportData addFindingsItem(ReportFinding findingsItem) {
    if (this.findings == null) {
      this.findings = new ArrayList<>();
    }
    this.findings.add(findingsItem);
    return this;
  }

  /**
   * Get findings
   * @return findings
   */
  @NotNull @Valid 
  @Schema(name = "findings", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("findings")
  public List<@Valid ReportFinding> getFindings() {
    return findings;
  }

  public void setFindings(List<@Valid ReportFinding> findings) {
    this.findings = findings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReportData reportData = (ReportData) o;
    return Objects.equals(this.scanId, reportData.scanId) &&
        Objects.equals(this.site, reportData.site) &&
        Objects.equals(this.scanStatus, reportData.scanStatus) &&
        Objects.equals(this.scanCreatedAt, reportData.scanCreatedAt) &&
        Objects.equals(this.scanCompletedAt, reportData.scanCompletedAt) &&
        Objects.equals(this.generatedAt, reportData.generatedAt) &&
        Objects.equals(this.safeToLaunch, reportData.safeToLaunch) &&
        Objects.equals(this.executiveSummary, reportData.executiveSummary) &&
        Objects.equals(this.findings, reportData.findings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scanId, site, scanStatus, scanCreatedAt, scanCompletedAt, generatedAt, safeToLaunch, executiveSummary, findings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReportData {\n");
    sb.append("    scanId: ").append(toIndentedString(scanId)).append("\n");
    sb.append("    site: ").append(toIndentedString(site)).append("\n");
    sb.append("    scanStatus: ").append(toIndentedString(scanStatus)).append("\n");
    sb.append("    scanCreatedAt: ").append(toIndentedString(scanCreatedAt)).append("\n");
    sb.append("    scanCompletedAt: ").append(toIndentedString(scanCompletedAt)).append("\n");
    sb.append("    generatedAt: ").append(toIndentedString(generatedAt)).append("\n");
    sb.append("    safeToLaunch: ").append(toIndentedString(safeToLaunch)).append("\n");
    sb.append("    executiveSummary: ").append(toIndentedString(executiveSummary)).append("\n");
    sb.append("    findings: ").append(toIndentedString(findings)).append("\n");
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

