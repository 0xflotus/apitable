package com.vikadata.api.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import com.vikadata.api.workspace.enums.NodeType;

@Data
@AllArgsConstructor
public class NodeData {

    private NodeType type;

    private String nodeId;

    private String nodeName;

    private String preNodeId;

    private String parentId;
}
