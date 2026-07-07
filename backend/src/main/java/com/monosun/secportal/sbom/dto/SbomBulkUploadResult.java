package com.monosun.secportal.sbom.dto;

import java.util.List;

public record SbomBulkUploadResult(
        int total,
        int success,
        int failed,
        int softwareCount,
        List<RowError> errors
) {
    public record RowError(int row, String message) {}
}
