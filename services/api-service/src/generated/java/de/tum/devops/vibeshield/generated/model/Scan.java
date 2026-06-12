package de.tum.devops.vibeshield.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A scan job and its current state.
 */

@Schema(name = "Scan", description = "A scan job and its current state.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class Scan {

  private Long id;

  private Long websiteId;

  private ScanStatus status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime completedAt = null;

  private @Nullable String errorMessage = null;

  private @Nullable Integer findingCount = null;

  public Scan() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Scan(Long id, Long websiteId, ScanStatus status, OffsetDateTime createdAt) {
    this.id = id;
    this.websiteId = websiteId;
    this.status = status;
    this.createdAt = createdAt;
  }

  public Scan id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @Schema(name = "id", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Scan websiteId(Long websiteId) {
    this.websiteId = websiteId;
    return this;
  }

  /**
   * Get websiteId
   * @return websiteId
   */
  @NotNull 
  @Schema(name = "websiteId", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("websiteId")
  public Long getWebsiteId() {
    return websiteId;
  }

  public void setWebsiteId(Long websiteId) {
    this.websiteId = websiteId;
  }

  public Scan status(ScanStatus status) {
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
  public ScanStatus getStatus() {
    return status;
  }

  public void setStatus(ScanStatus status) {
    this.status = status;
  }

  public Scan createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @Schema(name = "createdAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Scan completedAt(@Nullable OffsetDateTime completedAt) {
    this.completedAt = completedAt;
    return this;
  }

  /**
   * Set once the status is `Completed` or `Failed`.
   * @return completedAt
   */
  @Valid 
  @Schema(name = "completedAt", description = "Set once the status is `Completed` or `Failed`.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("completedAt")
  public @Nullable OffsetDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(@Nullable OffsetDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public Scan errorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Why the scan failed; only set when the status is `Failed`.
   * @return errorMessage
   */
  
  @Schema(name = "errorMessage", description = "Why the scan failed; only set when the status is `Failed`.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorMessage")
  public @Nullable String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Scan findingCount(@Nullable Integer findingCount) {
    this.findingCount = findingCount;
    return this;
  }

  /**
   * Number of findings; only set when the status is `Completed`.
   * @return findingCount
   */
  
  @Schema(name = "findingCount", example = "5", description = "Number of findings; only set when the status is `Completed`.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("findingCount")
  public @Nullable Integer getFindingCount() {
    return findingCount;
  }

  public void setFindingCount(@Nullable Integer findingCount) {
    this.findingCount = findingCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Scan scan = (Scan) o;
    return Objects.equals(this.id, scan.id) &&
        Objects.equals(this.websiteId, scan.websiteId) &&
        Objects.equals(this.status, scan.status) &&
        Objects.equals(this.createdAt, scan.createdAt) &&
        Objects.equals(this.completedAt, scan.completedAt) &&
        Objects.equals(this.errorMessage, scan.errorMessage) &&
        Objects.equals(this.findingCount, scan.findingCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, websiteId, status, createdAt, completedAt, errorMessage, findingCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Scan {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    websiteId: ").append(toIndentedString(websiteId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    completedAt: ").append(toIndentedString(completedAt)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    findingCount: ").append(toIndentedString(findingCount)).append("\n");
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

