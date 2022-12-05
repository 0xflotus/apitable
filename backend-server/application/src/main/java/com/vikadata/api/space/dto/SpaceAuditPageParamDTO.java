package com.vikadata.api.space.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ApiModel("Space Audit Page")
public class SpaceAuditPageParamDTO {

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private List<Long> memberIds;

    private List<String> actions;

    private String keyword;

    private Integer pageNo;

    private Integer pageSize;

    private List<String> nodeIds;
}
