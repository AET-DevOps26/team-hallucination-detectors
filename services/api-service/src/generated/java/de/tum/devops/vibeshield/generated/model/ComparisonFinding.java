package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tum.devops.vibeshield.generated.model.EffortEstimate;
import de.tum.devops.vibeshield.generated.model.ScanChangeStatus;
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
 * ComparisonFinding
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ComparisonFinding {

  private @Nullable Long findingId = null;

  private ScanChangeStatus changeStatus;

  private Severity severity;

  private ScanCheck check;

  private String title;

  private String affected;

  private String suggestedFix;

  private @Nullable Integer suggestedFixOrder = null;

  private EffortEstimate effort;

  public ComparisonFinding() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComparisonFinding(ScanChangeStatus changeStatus, Severity severity, ScanCheck check, String title, String affected, String suggestedFix, EffortEstimate effort) {
    this.changeStatus = changeStatus;
    this.severity = severity;
    this.check = check;
    this.title = title;
    this.affected = affected;
    this.suggestedFix = suggestedFix;
    this.effort = effort;
  }

  public ComparisonFinding findingId(@Nullable Long findingId) {
    this.findingId = findingId;
    return this;
  }

  /**
   * ID of the finding in the scan it came from.
   * @return findingId
   */
  
  @Schema(name = "findingId", description = "ID of the finding in the scan it came from.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("findingId")
  public @Nullable Long getFindingId() {
    return findingId;
  }

  public void setFindingId(@Nullable Long findingId) {
    this.findingId = findingId;
  }

  public ComparisonFinding changeStatus(ScanChangeStatus changeStatus) {
    this.changeStatus = changeStatus;
    return this;
  }

  /**
   * Get changeStatus
   * @return changeStatus
   */
  @NotNull @Valid 
  @Schema(name = "changeStatus", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("changeStatus")
  public ScanChangeStatus getChangeStatus() {
    return changeStatus;
  }

  public void setChangeStatus(ScanChangeStatus changeStatus) {
    this.changeStatus = changeStatus;
  }

  public ComparisonFinding severity(Severity severity) {
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

  public ComparisonFinding check(ScanCheck check) {
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

  public ComparisonFinding title(String title) {
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

  public ComparisonFinding affected(String affected) {
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

  public ComparisonFinding suggestedFix(String suggestedFix) {
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

  public ComparisonFinding suggestedFixOrder(@Nullable Integer suggestedFixOrder) {
    this.suggestedFixOrder = suggestedFixOrder;
    return this;
  }

  /**
   * 1-based action-plan order for current open findings.
   * @return suggestedFixOrder
   */
  
  @Schema(name = "suggestedFixOrder", description = "1-based action-plan order for current open findings.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("suggestedFixOrder")
  public @Nullable Integer getSuggestedFixOrder() {
    return suggestedFixOrder;
  }

  public void setSuggestedFixOrder(@Nullable Integer suggestedFixOrder) {
    this.suggestedFixOrder = suggestedFixOrder;
  }

  public ComparisonFinding effort(EffortEstimate effort) {
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
    ComparisonFinding comparisonFinding = (ComparisonFinding) o;
    return Objects.equals(this.findingId, comparisonFinding.findingId) &&
        Objects.equals(this.changeStatus, comparisonFinding.changeStatus) &&
        Objects.equals(this.severity, comparisonFinding.severity) &&
        Objects.equals(this.check, comparisonFinding.check) &&
        Objects.equals(this.title, comparisonFinding.title) &&
        Objects.equals(this.affected, comparisonFinding.affected) &&
        Objects.equals(this.suggestedFix, comparisonFinding.suggestedFix) &&
        Objects.equals(this.suggestedFixOrder, comparisonFinding.suggestedFixOrder) &&
        Objects.equals(this.effort, comparisonFinding.effort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(findingId, changeStatus, severity, check, title, affected, suggestedFix, suggestedFixOrder, effort);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComparisonFinding {\n");
    sb.append("    findingId: ").append(toIndentedString(findingId)).append("\n");
    sb.append("    changeStatus: ").append(toIndentedString(changeStatus)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    check: ").append(toIndentedString(check)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    affected: ").append(toIndentedString(affected)).append("\n");
    sb.append("    suggestedFix: ").append(toIndentedString(suggestedFix)).append("\n");
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

