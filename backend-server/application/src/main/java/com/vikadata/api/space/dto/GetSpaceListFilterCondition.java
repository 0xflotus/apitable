package com.vikadata.api.space.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("Space Filter Condition")
public class GetSpaceListFilterCondition {

    private Boolean manageable;
}
