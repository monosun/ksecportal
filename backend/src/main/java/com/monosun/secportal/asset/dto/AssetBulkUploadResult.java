package com.monosun.secportal.asset.dto;

import java.util.List;

public record AssetBulkUploadResult(
        int total,
        int success,
        int failed,
        List<RowError> errors
) {
    public record RowError(int row, String message) {}
}
