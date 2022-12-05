package com.vikadata.api.space.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import com.vikadata.api.base.enums.ParameterException;
import com.vikadata.api.interfaces.billing.facade.EntitlementServiceFacade;
import com.vikadata.api.interfaces.billing.model.SubscriptionInfo;
import com.vikadata.api.shared.component.adapter.MultiDatasourceAdapterTemplate;
import com.vikadata.api.shared.sysconfig.SystemConfigManager;
import com.vikadata.api.shared.util.page.PageHelper;
import com.vikadata.api.shared.util.page.PageInfo;
import com.vikadata.api.space.enums.SubscribeFunctionException;
import com.vikadata.api.space.dto.SpaceAuditPageParamDTO;
import com.vikadata.api.space.vo.SpaceAuditPageVO;
import com.vikadata.api.space.service.ISpaceAuditService;
import com.vikadata.api.workspace.mapper.NodeMapper;
import com.vikadata.core.util.ExceptionUtil;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SpaceAuditServiceImpl implements ISpaceAuditService {

    private static final List<String> showAudits = SystemConfigManager.getConfig().getAudit().entrySet().stream()
            .filter((entry) -> entry.getValue().isShowInAuditLog())
            .map(Entry::getKey).collect(Collectors.toList());

    @Resource
    private EntitlementServiceFacade entitlementServiceFacade;

    @Resource
    private NodeMapper nodeMapper;

    @Resource
    private MultiDatasourceAdapterTemplate multiDatasourceAdapterTemplate;

    @Override
    public PageInfo<SpaceAuditPageVO> getSpaceAuditPageVO(String spaceId, SpaceAuditPageParamDTO param) {
        // Gets the number of days the space subscription plan is available for audit query
        SubscriptionInfo subscriptionInfo = entitlementServiceFacade.getSpaceSubscription(spaceId);
        long queryDays = subscriptionInfo.getFeature().getAuditQueryDays().getValue();
        LocalDateTime today = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        LocalDateTime beginTime = param.getBeginTime();
        // check start time
        if (beginTime != null) {
            long between = LocalDateTimeUtil.between(beginTime, today, ChronoUnit.DAYS);
            ExceptionUtil.isTrue(queryDays >= between, SubscribeFunctionException.AUDIT_LIMIT);
        }
        else {
            beginTime = today.plusDays(1 - queryDays);
            param.setBeginTime(beginTime);
        }
        // check end time
        if (param.getEndTime() != null) {
            ExceptionUtil.isFalse(LocalDateTimeUtil.between(beginTime, param.getEndTime()).isNegative(), ParameterException.INCORRECT_ARG);
        }

        // file search
        String likeName = StrUtil.trim(param.getKeyword());
        if (StrUtil.isNotBlank(likeName)) {
            // fuzzy search node
            List<String> nodeIds = nodeMapper.selectNodeIdBySpaceIdAndNodeNameLikeIncludeDeleted(spaceId, likeName);
            // The result is empty, ending the return
            if (nodeIds.isEmpty()) {
                return PageHelper.build(param.getPageNo(), param.getPageSize(), 0, new ArrayList<>());
            }
            param.setNodeIds(nodeIds);
        }

        if (CollUtil.isEmpty(param.getActions()) && !showAudits.isEmpty()) {
            param.setActions(showAudits);
        }

        return multiDatasourceAdapterTemplate.getSpaceAuditPage(spaceId, param);
    }

}
