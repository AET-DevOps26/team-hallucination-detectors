package de.tum.devops.vibeshield.rescan;

import com.fasterxml.jackson.annotation.JsonValue;

/** How a finding changed compared with the previous completed scan. */
public enum ScanChangeStatus {
    FIXED("Fixed"),
    STILL_PRESENT("Still present"),
    NEWLY_INTRODUCED("Newly introduced");

    private final String value;

    ScanChangeStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
