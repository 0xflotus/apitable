package com.vikadata.api.space.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.extern.slf4j.Slf4j;

import com.vikadata.api.space.mapper.SpaceRoleResourceRelMapper;
import com.vikadata.api.space.service.ISpaceRoleResourceRelService;
import com.vikadata.core.util.ExceptionUtil;
import com.vikadata.entity.SpaceRoleResourceRelEntity;

import org.springframework.stereotype.Service;

import static com.vikadata.api.workspace.enums.PermissionException.CREATE_SUB_ADMIN_ERROR;
import static com.vikadata.api.workspace.enums.PermissionException.DELETE_ROLE_ERROR;
import static com.vikadata.api.workspace.enums.PermissionException.UPDATE_ROLE_ERROR;

@Service
@Slf4j
public class SpaceRoleResourceRelServiceImpl extends ServiceImpl<SpaceRoleResourceRelMapper, SpaceRoleResourceRelEntity> implements ISpaceRoleResourceRelService {

    @Override
    public void createBatch(List<String> roleCodes, List<String> resourceCodes) {
        log.info("Batch insert the role permission association.");
        List<SpaceRoleResourceRelEntity> entityList = new ArrayList<>();
        for (String roleCode : roleCodes) {
            for (String resourceCode : resourceCodes) {
                SpaceRoleResourceRelEntity roleResourceRel = new SpaceRoleResourceRelEntity();
                roleResourceRel.setId(IdWorker.getId());
                roleResourceRel.setRoleCode(roleCode);
                roleResourceRel.setResourceCode(resourceCode);
                entityList.add(roleResourceRel);
            }
        }
        boolean flag = SqlHelper.retBool(baseMapper.insertBatch(entityList));
        ExceptionUtil.isTrue(flag, CREATE_SUB_ADMIN_ERROR);
    }

    @Override
    public void delete(String roleCode) {
        log.info("Delete role permission");
        boolean flag = SqlHelper.retBool(baseMapper.deleteByRoleCode(roleCode));
        ExceptionUtil.isTrue(flag, DELETE_ROLE_ERROR);
    }

    @Override
    public void deleteBatch(String roleCode, List<String> resourceCodes) {
        log.info("Batch deleting role permission");
        boolean flag = SqlHelper.retBool(baseMapper.deleteByRoleCodeAndResourceCodes(roleCode, resourceCodes));
        ExceptionUtil.isTrue(flag, UPDATE_ROLE_ERROR);
    }
}
