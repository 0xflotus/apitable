package com.apitable.starter.vika.core.model;

/**
 * <p>
 * DingTalk config template
 * </p>
 *
 */
public class DingTalkDaTemplateInfo {

    private String templateId;

    private String iconUrl;

    private String iconName;

    private String templateName;

    private String iconMediaId;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getIconMediaId() {
        return iconMediaId;
    }

    public void setIconMediaId(String iconMediaId) {
        this.iconMediaId = iconMediaId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
}
