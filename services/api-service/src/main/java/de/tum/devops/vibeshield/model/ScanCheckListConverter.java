package de.tum.devops.vibeshield.model;

import de.tum.devops.vibeshield.generated.model.ScanCheck;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persists the list of requested checks as a comma-separated string of enum constant
 * names — a scan's check selection is only ever read back as a whole, never queried.
 */
@Converter
public class ScanCheckListConverter implements AttributeConverter<List<ScanCheck>, String> {

    @Override
    public String convertToDatabaseColumn(List<ScanCheck> checks) {
        return checks.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public List<ScanCheck> convertToEntityAttribute(String column) {
        return Arrays.stream(column.split(",")).map(ScanCheck::valueOf).toList();
    }
}
