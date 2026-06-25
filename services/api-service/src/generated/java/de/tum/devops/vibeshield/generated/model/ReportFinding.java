package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tum.devops.vibeshield.generated.model.EffortEstimate;
import de.tum.devops.vibeshield.generated.model.FindingStatus;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.Severity;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ReportFinding
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ReportFinding {

  private Long id;

  private Severity severity;

  private ScanCheck check;

  private String title;

  private String affected;

  private String explanation;

  private String suggestedFix;

  private FindingStatus status;

  private @Nullable Integer suggestedFixOrder = null;

  private EffortEstimate effort;

  public ReportFinding() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReportFinding(Long id, Severity severity, ScanCheck check, String title, String affected, String explanation, String suggestedFix, FindingStatus status, EffortEstimate effort) {
    this.id = id;
    this.severity = severity;
    this.check = check;
    this.title = title;
    this.affected = affected;
    this.explanation = explanation;
    this.suggestedFix = suggestedFix;
    this.status = status;
    this.effort = effort;
  }

  public ReportFinding id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ReportFinding severity(Severity severity) {
    this.severity = severity;
    return this;
  }

  /**
   * Get severity
   * @return severity
   */
  @NotNull @Valid 
  @Schema(name = "severity", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("severity")
  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public ReportFinding check(ScanCheck check) {
    this.check = check;
    return this;
  }

  /**
   * Get check
   * @return check
   */
  @NotNull @Valid 
  @Schema(name = "check", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("check")
  public ScanCheck getCheck() {
    return check;
  }

  public void setCheck(ScanCheck check) {
    this.check = check;
  }

  public ReportFinding title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  @NotNull 
  @Schema(name = "title", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ReportFinding affected(String affected) {
    this.affected = affected;
    return this;
  }

  /**
   * Get affected
   * @return affected
   */
  @NotNull 
  @Schema(name = "affected", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("affected")
  public String getAffected() {
    return affected;
  }

  public void setAffected(String affected) {
    this.affected = affected;
  }

  public ReportFinding explanation(String explanation) {
    this.explanation = explanation;
    return this;
  }

  /**
   * Get explanation
   * @return explanation
   */
  @NotNull 
  @Schema(name = "explanation", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("explanation")
  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public ReportFinding suggestedFix(String suggestedFix) {
    this.suggestedFix = suggestedFix;
    return this;
  }

  /**
   * Get suggestedFix
   * @return suggestedFix
   */
  @NotNull 
  @Schema(name = "suggestedFix", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("suggestedFix")
  public String getSuggestedFix() {
    return suggestedFix;
  }

  public void setSuggestedFix(String suggestedFix) {
    this.suggestedFix = suggestedFix;
  }

  public ReportFinding status(FindingStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public FindingStatus getStatus() {
    return status;
  }

  public void setStatus(FindingStatus status) {
    this.status = status;
  }

  public ReportFinding suggestedFixOrder(@Nullable Integer suggestedFixOrder) {
    this.suggestedFixOrder = suggestedFixOrder;
    return this;
  }

  /**
   * 1-based priority among open findings; null for fixed or ignored findings.
   * @return suggestedFixOrder
   */
  
  @Schema(name = "suggestedFixOrder", description = "1-based priority among open findings; null for fixed or ignored findings.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("suggestedFixOrder")
  public @Nullable Integer getSuggestedFixOrder() {
    return suggestedFixOrder;
  }

  public void setSuggestedFixOrder(@Nullable Integer suggestedFixOrder) {
    this.suggestedFixOrder = suggestedFixOrder;
  }

  public ReportFinding effort(EffortEstimate effort) {
    this.effort = effort;
    return this;
  }

  /**
   * Get effort
   * @return effort
   */
  @NotNull @Valid 
  @Schema(name = "effort", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("effort")
  public EffortEstimate getEffort() {
    return effort;
  }

  public void setEffort(EffortEstimate effort) {
    this.effort = effort;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReportFinding reportFinding = (ReportFinding) o;
    return Objects.equals(this.id, reportFinding.id) &&
        Objects.equals(this.severity, reportFinding.severity) &&
        Objects.equals(this.check, reportFinding.check) &&
        Objects.equals(this.title, reportFinding.title) &&
        Objects.equals(this.affected, reportFinding.affected) &&
        Objects.equals(this.explanation, reportFinding.explanation) &&
        Objects.equals(this.suggestedFix, reportFinding.suggestedFix) &&
        Objects.equals(this.status, reportFinding.status) &&
        Objects.equals(this.suggestedFixOrder, reportFinding.suggestedFixOrder) &&
        Objects.equals(this.effort, reportFinding.effort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, severity, check, title, affected, explanation, suggestedFix, status, suggestedFixOrder, effort);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReportFinding {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    check: ").append(toIndentedString(check)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    affected: ").append(toIndentedString(affected)).append("\n");
    sb.append("    explanation: ").append(toIndentedString(explanation)).append("\n");
    sb.append("    suggestedFix: ").append(toIndentedString(suggestedFix)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    suggestedFixOrder: ").append(toIndentedString(suggestedFixOrder)).append("\n");
    sb.append("    effort: ").append(toIndentedString(effort)).append("\n");
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

