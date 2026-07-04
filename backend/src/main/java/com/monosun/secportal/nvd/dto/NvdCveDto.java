package com.monosun.secportal.nvd.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NvdCveDto {
    private String cveId;
    private String description;
    private Double cvssScore;
    private String severity;
}
