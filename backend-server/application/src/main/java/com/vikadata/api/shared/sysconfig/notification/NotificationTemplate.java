package com.vikadata.api.shared.sysconfig.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * <p>
 * Notification Template
 * </p>
 */
@Data
public class NotificationTemplate {
    private String id;

    private boolean canJump;

    private String toTag;

    private String formatString;

    @JsonProperty("is_notification")
    private boolean isNotification;

    @JsonProperty("is_mobile")
    private boolean isMobile;

    @JsonProperty("is_browser")
    private boolean isBrowser;

    @JsonProperty("is_component")
    private boolean isComponent;

    @JsonProperty("is_mail")
    private boolean isMail;

    private String url;

    private String notificationsType;

    private String mailTemplateSubject;

    private Integer frequency;
}
