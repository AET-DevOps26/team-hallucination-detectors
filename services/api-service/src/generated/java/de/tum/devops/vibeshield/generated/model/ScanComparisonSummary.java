package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ScanComparisonSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ScanComparisonSummary {

  private Integer fixed;

  private Integer stillPresent;

  private Integer newlyIntroduced;

  public ScanComparisonSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScanComparisonSummary(Integer fixed, Integer stillPresent, Integer newlyIntroduced) {
    this.fixed = fixed;
    this.stillPresent = stillPresent;
    this.newlyIntroduced = newlyIntroduced;
  }

  public ScanComparisonSummary fixed(Integer fixed) {
    this.fixed = fixed;
    return this;
  }

  /**
   * Get fixed
   * @return fixed
   */
  @NotNull 
  @Schema(name = "fixed", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("fixed")
  public Integer getFixed() {
    return fixed;
  }

  public void setFixed(Integer fixed) {
    this.fixed = fixed;
  }

  public ScanComparisonSummary stillPresent(Integer stillPresent) {
    this.stillPresent = stillPresent;
    return this;
  }

  /**
   * Get stillPresent
   * @return stillPresent
   */
  @NotNull 
  @Schema(name = "stillPresent", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("stillPresent")
  public Integer getStillPresent() {
    return stillPresent;
  }

  public void setStillPresent(Integer stillPresent) {
    this.stillPresent = stillPresent;
  }

  public ScanComparisonSummary newlyIntroduced(Integer newlyIntroduced) {
    this.newlyIntroduced = newlyIntroduced;
    return this;
  }

  /**
   * Get newlyIntroduced
   * @return newlyIntroduced
   */
  @NotNull 
  @Schema(name = "newlyIntroduced", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("newlyIntroduced")
  public Integer getNewlyIntroduced() {
    return newlyIntroduced;
  }

  public void setNewlyIntroduced(Integer newlyIntroduced) {
    this.newlyIntroduced = newlyIntroduced;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScanComparisonSummary scanComparisonSummary = (ScanComparisonSummary) o;
    return Objects.equals(this.fixed, scanComparisonSummary.fixed) &&
        Objects.equals(this.stillPresent, scanComparisonSummary.stillPresent) &&
        Objects.equals(this.newlyIntroduced, scanComparisonSummary.newlyIntroduced);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fixed, stillPresent, newlyIntroduced);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScanComparisonSummary {\n");
    sb.append("    fixed: ").append(toIndentedString(fixed)).append("\n");
    sb.append("    stillPresent: ").append(toIndentedString(stillPresent)).append("\n");
    sb.append("    newlyIntroduced: ").append(toIndentedString(newlyIntroduced)).append("\n");
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

