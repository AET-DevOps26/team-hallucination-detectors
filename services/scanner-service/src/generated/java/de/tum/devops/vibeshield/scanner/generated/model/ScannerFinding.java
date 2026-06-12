package de.tum.devops.vibeshield.scanner.generated.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A single discovered issue. The backend assigns IDs and persistence concerns; the scanner only reports content (issue #27). 
 */

@Schema(name = "ScannerFinding", description = "A single discovered issue. The backend assigns IDs and persistence concerns; the scanner only reports content (issue #27). ")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public class ScannerFinding {

  private ScanCheck check;

  private String title;

  private Severity severity;

  private String affected;

  private String explanation;

  private String suggestedFix;

  public ScannerFinding() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScannerFinding(ScanCheck check, String title, Severity severity, String affected, String explanation, String suggestedFix) {
    this.check = check;
    this.title = title;
    this.severity = severity;
    this.affected = affected;
    this.explanation = explanation;
    this.suggestedFix = suggestedFix;
  }

  public ScannerFinding check(ScanCheck check) {
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

  public ScannerFinding title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  @NotNull 
  @Schema(name = "title", example = "Missing Content-Security-Policy header", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ScannerFinding severity(Severity severity) {
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

  public ScannerFinding affected(String affected) {
    this.affected = affected;
    return this;
  }

  /**
   * URL, path, or header the finding applies to.
   * @return affected
   */
  @NotNull 
  @Schema(name = "affected", example = "https://my-vibecoded-shop.lovable.app/", description = "URL, path, or header the finding applies to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("affected")
  public String getAffected() {
    return affected;
  }

  public void setAffected(String affected) {
    this.affected = affected;
  }

  public ScannerFinding explanation(String explanation) {
    this.explanation = explanation;
    return this;
  }

  /**
   * Non-technical explanation of the risk, written for vibecoders.
   * @return explanation
   */
  @NotNull 
  @Schema(name = "explanation", description = "Non-technical explanation of the risk, written for vibecoders.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("explanation")
  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public ScannerFinding suggestedFix(String suggestedFix) {
    this.suggestedFix = suggestedFix;
    return this;
  }

  /**
   * Plain-language fix suggestion; input for AI fix-prompt generation.
   * @return suggestedFix
   */
  @NotNull 
  @Schema(name = "suggestedFix", description = "Plain-language fix suggestion; input for AI fix-prompt generation.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("suggestedFix")
  public String getSuggestedFix() {
    return suggestedFix;
  }

  public void setSuggestedFix(String suggestedFix) {
    this.suggestedFix = suggestedFix;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScannerFinding scannerFinding = (ScannerFinding) o;
    return Objects.equals(this.check, scannerFinding.check) &&
        Objects.equals(this.title, scannerFinding.title) &&
        Objects.equals(this.severity, scannerFinding.severity) &&
        Objects.equals(this.affected, scannerFinding.affected) &&
        Objects.equals(this.explanation, scannerFinding.explanation) &&
        Objects.equals(this.suggestedFix, scannerFinding.suggestedFix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(check, title, severity, affected, explanation, suggestedFix);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScannerFinding {\n");
    sb.append("    check: ").append(toIndentedString(check)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    affected: ").append(toIndentedString(affected)).append("\n");
    sb.append("    explanation: ").append(toIndentedString(explanation)).append("\n");
    sb.append("    suggestedFix: ").append(toIndentedString(suggestedFix)).append("\n");
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

