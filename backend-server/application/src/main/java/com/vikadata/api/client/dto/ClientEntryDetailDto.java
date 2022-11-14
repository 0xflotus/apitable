package com.vikadata.api.client.dto;

import lombok.Data;

@Data
public class ClientEntryDetailDto {

    /**
     * ID
     */
    private Long id;

    /**
     * htmlContent
     */
    private String htmlContent;

    private String version;

    private String publishUser;

    private String description;
}
