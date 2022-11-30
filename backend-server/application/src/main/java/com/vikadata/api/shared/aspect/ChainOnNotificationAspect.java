package com.vikadata.api.shared.aspect;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

import com.vikadata.api.shared.component.TaskManager;
import com.vikadata.api.shared.component.notification.NotificationHelper;
import com.vikadata.api.shared.component.notification.NotificationManager;
import com.vikadata.api.shared.component.notification.NotificationRenderField;
import com.vikadata.api.shared.component.notification.NotificationTemplateId;
import com.vikadata.api.shared.component.notification.annotation.Notification;
import com.vikadata.api.shared.context.LoginContext;
import com.vikadata.api.shared.context.SessionContext;
import com.vikadata.api.shared.holder.NotificationRenderFieldHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <p>
 * notification aop
 * </p>
 *
 * @author zoe zheng
 */
@Aspect
@Component
@Slf4j
public class ChainOnNotificationAspect extends BaseAspectSupport {

    @Resource
    private NotificationManager notificationManager;

    @AfterReturning(pointcut = "@annotation(notification)", returning = "result")
    public void doAfterReturning(Notification notification, Object result) {
        try {
            NotificationRenderField renderField = NotificationRenderFieldHolder.get();

            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            boolean isNodeOperate = NotificationHelper.isNodeOperate(request.getServletPath());
            final String spaceId;
            final Long fromUserId;
            final List<Long> playerIds;
            final Map<String, Object> bodyExtras;
            if (renderField != null) {
                spaceId = StrUtil.isNotBlank(renderField.getSpaceId()) ? renderField.getSpaceId() : LoginContext.me().getSpaceId();
                fromUserId = ObjectUtil.isNotNull(renderField.getFromUserId()) ? renderField.getFromUserId() : SessionContext.getUserId();
                playerIds = renderField.getPlayerIds();
                bodyExtras = renderField.getBodyExtras();
            }
            else {
                spaceId = LoginContext.me().getSpaceId();
                fromUserId = SessionContext.getUserId();
                playerIds = null;
                bodyExtras = null;
            }
            for (NotificationTemplateId templateId : notification.templateId()) {
                if (isNodeOperate) {
                    TaskManager.me().execute(() -> notificationManager.spaceNotify(templateId, fromUserId, spaceId, result));
                }
                else {
                    TaskManager.me().execute(() -> notificationManager.playerNotify(templateId, playerIds, fromUserId, spaceId, bodyExtras));
                }
            }
        }
        catch (Exception e) {
            log.error("PlayerNotification:Error", e);
        }
    }
}
