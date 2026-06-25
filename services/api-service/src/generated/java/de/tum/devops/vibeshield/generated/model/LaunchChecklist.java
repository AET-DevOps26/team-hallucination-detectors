package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tum.devops.vibeshield.generated.model.LaunchChecklistItem;
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
 * LaunchChecklist
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class LaunchChecklist {

  /**
   * Gets or Sets status
   */
  public enum StatusEnum {
    SAFE_TO_LAUNCH("Safe to launch"),
    
    SAFE_WITH_WARNINGS("Safe with warnings"),
    
    NEEDS_ATTENTION("Needs attention"),
    
    NOT_SAFE_TO_LAUNCH("Not safe to launch");

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

  private Integer blockingIssues;

  @Valid
  private List<@Valid LaunchChecklistItem> items = new ArrayList<>();

  public LaunchChecklist() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LaunchChecklist(StatusEnum status, Integer blockingIssues, List<@Valid LaunchChecklistItem> items) {
    this.status = status;
    this.blockingIssues = blockingIssues;
    this.items = items;
  }

  public LaunchChecklist status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public LaunchChecklist blockingIssues(Integer blockingIssues) {
    this.blockingIssues = blockingIssues;
    return this;
  }

  /**
   * Get blockingIssues
   * @return blockingIssues
   */
  @NotNull 
  @Schema(name = "blockingIssues", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("blockingIssues")
  public Integer getBlockingIssues() {
    return blockingIssues;
  }

  public void setBlockingIssues(Integer blockingIssues) {
    this.blockingIssues = blockingIssues;
  }

  public LaunchChecklist items(List<@Valid LaunchChecklistItem> items) {
    this.items = items;
    return this;
  }

  public LaunchChecklist addItemsItem(LaunchChecklistItem itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
   */
  @NotNull @Valid 
  @Schema(name = "items", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("items")
  public List<@Valid LaunchChecklistItem> getItems() {
    return items;
  }

  public void setItems(List<@Valid LaunchChecklistItem> items) {
    this.items = items;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LaunchChecklist launchChecklist = (LaunchChecklist) o;
    return Objects.equals(this.status, launchChecklist.status) &&
        Objects.equals(this.blockingIssues, launchChecklist.blockingIssues) &&
        Objects.equals(this.items, launchChecklist.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, blockingIssues, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LaunchChecklist {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    blockingIssues: ").append(toIndentedString(blockingIssues)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

