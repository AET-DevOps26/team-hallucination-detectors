package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tum.devops.vibeshield.generated.model.EffortEstimate;
import de.tum.devops.vibeshield.generated.model.Severity;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ReportNextStep
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ReportNextStep {

  private Integer order;

  private String title;

  private Severity severity;

  private String affected;

  private EffortEstimate effort;

  private String action;

  public ReportNextStep() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReportNextStep(Integer order, String title, Severity severity, String affected, EffortEstimate effort, String action) {
    this.order = order;
    this.title = title;
    this.severity = severity;
    this.affected = affected;
    this.effort = effort;
    this.action = action;
  }

  public ReportNextStep order(Integer order) {
    this.order = order;
    return this;
  }

  /**
   * Get order
   * @return order
   */
  @NotNull 
  @Schema(name = "order", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("order")
  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public ReportNextStep title(String title) {
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

  public ReportNextStep severity(Severity severity) {
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

  public ReportNextStep affected(String affected) {
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

  public ReportNextStep effort(EffortEstimate effort) {
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

  public ReportNextStep action(String action) {
    this.action = action;
    return this;
  }

  /**
   * Get action
   * @return action
   */
  @NotNull 
  @Schema(name = "action", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("action")
  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReportNextStep reportNextStep = (ReportNextStep) o;
    return Objects.equals(this.order, reportNextStep.order) &&
        Objects.equals(this.title, reportNextStep.title) &&
        Objects.equals(this.severity, reportNextStep.severity) &&
        Objects.equals(this.affected, reportNextStep.affected) &&
        Objects.equals(this.effort, reportNextStep.effort) &&
        Objects.equals(this.action, reportNextStep.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(order, title, severity, affected, effort, action);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReportNextStep {\n");
    sb.append("    order: ").append(toIndentedString(order)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    affected: ").append(toIndentedString(affected)).append("\n");
    sb.append("    effort: ").append(toIndentedString(effort)).append("\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
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

