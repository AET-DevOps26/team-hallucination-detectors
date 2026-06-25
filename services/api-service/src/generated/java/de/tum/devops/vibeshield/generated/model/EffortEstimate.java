package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * EffortEstimate
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class EffortEstimate {

  /**
   * Gets or Sets level
   */
  public enum LevelEnum {
    LOW("Low"),
    
    MEDIUM("Medium"),
    
    HIGH("High");

    private final String value;

    LevelEnum(String value) {
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
    public static LevelEnum fromValue(String value) {
      for (LevelEnum b : LevelEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private LevelEnum level;

  private String estimate;

  public EffortEstimate() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EffortEstimate(LevelEnum level, String estimate) {
    this.level = level;
    this.estimate = estimate;
  }

  public EffortEstimate level(LevelEnum level) {
    this.level = level;
    return this;
  }

  /**
   * Get level
   * @return level
   */
  @NotNull 
  @Schema(name = "level", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("level")
  public LevelEnum getLevel() {
    return level;
  }

  public void setLevel(LevelEnum level) {
    this.level = level;
  }

  public EffortEstimate estimate(String estimate) {
    this.estimate = estimate;
    return this;
  }

  /**
   * Get estimate
   * @return estimate
   */
  @NotNull 
  @Schema(name = "estimate", example = "1-2 hours", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("estimate")
  public String getEstimate() {
    return estimate;
  }

  public void setEstimate(String estimate) {
    this.estimate = estimate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EffortEstimate effortEstimate = (EffortEstimate) o;
    return Objects.equals(this.level, effortEstimate.level) &&
        Objects.equals(this.estimate, effortEstimate.estimate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(level, estimate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EffortEstimate {\n");
    sb.append("    level: ").append(toIndentedString(level)).append("\n");
    sb.append("    estimate: ").append(toIndentedString(estimate)).append("\n");
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

