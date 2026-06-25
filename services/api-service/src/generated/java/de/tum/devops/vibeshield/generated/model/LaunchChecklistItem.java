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
 * LaunchChecklistItem
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class LaunchChecklistItem {

  private String label;

  /**
   * Launch-readiness result for this scan category.
   */
  public enum ResultEnum {
    PASS("Pass"),
    
    NEEDS_ATTENTION("Needs attention"),
    
    NOT_SELECTED("Not selected"),
    
    INCOMPLETE("Incomplete");

    private final String value;

    ResultEnum(String value) {
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
    public static ResultEnum fromValue(String value) {
      for (ResultEnum b : ResultEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private ResultEnum result;

  private Boolean checked;

  private String reason;

  public LaunchChecklistItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LaunchChecklistItem(String label, ResultEnum result, Boolean checked, String reason) {
    this.label = label;
    this.result = result;
    this.checked = checked;
    this.reason = reason;
  }

  public LaunchChecklistItem label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  @NotNull 
  @Schema(name = "label", example = "HTTPS and mixed-content checks", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public LaunchChecklistItem result(ResultEnum result) {
    this.result = result;
    return this;
  }

  /**
   * Launch-readiness result for this scan category.
   * @return result
   */
  @NotNull 
  @Schema(name = "result", description = "Launch-readiness result for this scan category.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("result")
  public ResultEnum getResult() {
    return result;
  }

  public void setResult(ResultEnum result) {
    this.result = result;
  }

  public LaunchChecklistItem checked(Boolean checked) {
    this.checked = checked;
    return this;
  }

  /**
   * Whether this scan category was selected and evaluated.
   * @return checked
   */
  @NotNull 
  @Schema(name = "checked", description = "Whether this scan category was selected and evaluated.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("checked")
  public Boolean getChecked() {
    return checked;
  }

  public void setChecked(Boolean checked) {
    this.checked = checked;
  }

  public LaunchChecklistItem reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Get reason
   * @return reason
   */
  @NotNull 
  @Schema(name = "reason", example = "Checked and found 1 open finding; highest severity is High.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LaunchChecklistItem launchChecklistItem = (LaunchChecklistItem) o;
    return Objects.equals(this.label, launchChecklistItem.label) &&
        Objects.equals(this.result, launchChecklistItem.result) &&
        Objects.equals(this.checked, launchChecklistItem.checked) &&
        Objects.equals(this.reason, launchChecklistItem.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, result, checked, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LaunchChecklistItem {\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    result: ").append(toIndentedString(result)).append("\n");
    sb.append("    checked: ").append(toIndentedString(checked)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

