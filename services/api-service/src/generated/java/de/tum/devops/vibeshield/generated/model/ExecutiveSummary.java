package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tum.devops.vibeshield.generated.model.ReportNextStep;
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
 * ExecutiveSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ExecutiveSummary {

  /**
   * Gets or Sets riskLevel
   */
  public enum RiskLevelEnum {
    CRITICAL("Critical"),
    
    HIGH("High"),
    
    MEDIUM("Medium"),
    
    LOW("Low");

    private final String value;

    RiskLevelEnum(String value) {
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
    public static RiskLevelEnum fromValue(String value) {
      for (RiskLevelEnum b : RiskLevelEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private RiskLevelEnum riskLevel;

  private Integer totalFindings;

  private Integer openFindings;

  @Valid
  private List<@Valid ReportNextStep> recommendedNextSteps = new ArrayList<>();

  public ExecutiveSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ExecutiveSummary(RiskLevelEnum riskLevel, Integer totalFindings, Integer openFindings, List<@Valid ReportNextStep> recommendedNextSteps) {
    this.riskLevel = riskLevel;
    this.totalFindings = totalFindings;
    this.openFindings = openFindings;
    this.recommendedNextSteps = recommendedNextSteps;
  }

  public ExecutiveSummary riskLevel(RiskLevelEnum riskLevel) {
    this.riskLevel = riskLevel;
    return this;
  }

  /**
   * Get riskLevel
   * @return riskLevel
   */
  @NotNull 
  @Schema(name = "riskLevel", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("riskLevel")
  public RiskLevelEnum getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(RiskLevelEnum riskLevel) {
    this.riskLevel = riskLevel;
  }

  public ExecutiveSummary totalFindings(Integer totalFindings) {
    this.totalFindings = totalFindings;
    return this;
  }

  /**
   * Get totalFindings
   * @return totalFindings
   */
  @NotNull 
  @Schema(name = "totalFindings", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("totalFindings")
  public Integer getTotalFindings() {
    return totalFindings;
  }

  public void setTotalFindings(Integer totalFindings) {
    this.totalFindings = totalFindings;
  }

  public ExecutiveSummary openFindings(Integer openFindings) {
    this.openFindings = openFindings;
    return this;
  }

  /**
   * Get openFindings
   * @return openFindings
   */
  @NotNull 
  @Schema(name = "openFindings", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("openFindings")
  public Integer getOpenFindings() {
    return openFindings;
  }

  public void setOpenFindings(Integer openFindings) {
    this.openFindings = openFindings;
  }

  public ExecutiveSummary recommendedNextSteps(List<@Valid ReportNextStep> recommendedNextSteps) {
    this.recommendedNextSteps = recommendedNextSteps;
    return this;
  }

  public ExecutiveSummary addRecommendedNextStepsItem(ReportNextStep recommendedNextStepsItem) {
    if (this.recommendedNextSteps == null) {
      this.recommendedNextSteps = new ArrayList<>();
    }
    this.recommendedNextSteps.add(recommendedNextStepsItem);
    return this;
  }

  /**
   * Get recommendedNextSteps
   * @return recommendedNextSteps
   */
  @NotNull @Valid 
  @Schema(name = "recommendedNextSteps", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("recommendedNextSteps")
  public List<@Valid ReportNextStep> getRecommendedNextSteps() {
    return recommendedNextSteps;
  }

  public void setRecommendedNextSteps(List<@Valid ReportNextStep> recommendedNextSteps) {
    this.recommendedNextSteps = recommendedNextSteps;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecutiveSummary executiveSummary = (ExecutiveSummary) o;
    return Objects.equals(this.riskLevel, executiveSummary.riskLevel) &&
        Objects.equals(this.totalFindings, executiveSummary.totalFindings) &&
        Objects.equals(this.openFindings, executiveSummary.openFindings) &&
        Objects.equals(this.recommendedNextSteps, executiveSummary.recommendedNextSteps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(riskLevel, totalFindings, openFindings, recommendedNextSteps);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecutiveSummary {\n");
    sb.append("    riskLevel: ").append(toIndentedString(riskLevel)).append("\n");
    sb.append("    totalFindings: ").append(toIndentedString(totalFindings)).append("\n");
    sb.append("    openFindings: ").append(toIndentedString(openFindings)).append("\n");
    sb.append("    recommendedNextSteps: ").append(toIndentedString(recommendedNextSteps)).append("\n");
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

