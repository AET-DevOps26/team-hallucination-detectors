package de.tum.devops.vibeshield.scannerclient.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Keep in sync with ScanCheck in openapi.yaml.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.14.0")
public enum ScanCheck {
  
  CRAWL("crawl"),
  
  HTTPS("https"),
  
  HEADERS("headers"),
  
  ADMIN_PATHS("adminPaths"),
  
  SECRETS("secrets"),
  
  SENSITIVE_FILES("sensitiveFiles"),
  
  COOKIES("cookies"),
  
  CORS("cors");

  private final String value;

  ScanCheck(String value) {
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
  public static ScanCheck fromValue(String value) {
    for (ScanCheck b : ScanCheck.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

